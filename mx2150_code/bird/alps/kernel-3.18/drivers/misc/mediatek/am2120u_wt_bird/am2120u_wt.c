#include<linux/init.h>
#include<linux/module.h>
#include <linux/device.h>
#include <linux/platform_device.h>

#include <linux/gpio.h>
#include <linux/pinctrl/consumer.h>
#include <linux/of.h>
#include <linux/of_address.h>
#include <linux/of_device.h>
#include <linux/of_gpio.h>
#include <linux/of_irq.h>
#include <linux/interrupt.h>
#include <asm-generic/gpio.h>

#include <linux/delay.h>
#include <linux/proc_fs.h>
#include <asm/io.h> 
#include <asm/uaccess.h>
#include <linux/i2c.h>
#include <linux/mutex.h>
#include <linux/dma-mapping.h>
#include <linux/kthread.h>
#include <linux/wakelock.h>
#include <linux/input.h>

#include "am2120u_wt.h"

/**********************************************************************************************/
/* configuration*/
/**********************************************************************************************/
#define AM2120U_WT_NAME	"am2120u_wt"

#define OPEN      (1)
#define CLOSE     (0)

#define AM2120U_WT_PTT_KEY_CLOSE      KEY_F8
#define AM2120U_WT_PTT_KEY_OPEN       KEY_F9

int am2120u_wt_is_opening_flag=0; //moudle 1= open or 0 = close
EXPORT_SYMBOL(am2120u_wt_is_opening_flag);

static int am2120u_wt_major;
static struct class *am2120u_wt_class = NULL;
static struct device *am2120u_wt_device = NULL;
static struct input_dev *kpd_am2120u_wt_dev=NULL;

static int cur_am2120u_wt_ptt_eint_state = OPEN;
static int cur_am2120u_wt_ptt_eint_gpio_state =0 ;
unsigned int am2120u_wt_ptt_eint_irq = 0;
static int am2120u_wt_ptt_eint_irq_flag=0;

static int cur_am2120u_wt_sq_eint_state = OPEN;
static int cur_am2120u_wt_sq_eint_gpio_state =0 ;
unsigned int am2120u_wt_sq_eint_irq = 0;
static int am2120u_wt_sq_eint_irq_flag=0;

/*工作队列*/
struct wake_lock am2120u_wt_ptt_eint_lock;
static struct work_struct am2120u_wt_ptt_eint_work;
static struct workqueue_struct * am2120u_wt_ptt_eint_workqueue = NULL;

struct wake_lock am2120u_wt_sq_eint_lock;
static struct work_struct am2120u_wt_sq_eint_work;
static struct workqueue_struct * am2120u_wt_sq_eint_workqueue = NULL;


static const struct of_device_id am2120u_wt_of_match[] = {
	{
		.compatible = "mediatek,am2120u_wt"
	},
	{},
};

static const struct of_device_id am2120u_wt_ptt_eint_of_match[] = {
	{
		.compatible = "mediatek, PTT_EINT-eint"
	},
	{},
};

static const struct of_device_id am2120u_wt_sq_eint_of_match[] = {
	{
		.compatible = "mediatek, AM2120U_WT_SQ_EINT-eint"
	},
	{},
};
/**********************************************************************************************/
/* KPD BEGIN*/
/**********************************************************************************************/
static int am2120u_wt_ptt_kpd_input_init(void)
{
	kpd_am2120u_wt_dev = input_allocate_device();
	if (kpd_am2120u_wt_dev==NULL) 
	{
		AM2120U_WT_ERR("kpd_am2120u_wt_dev : fail!\n");
		return -1;
	}
	__set_bit(EV_KEY, kpd_am2120u_wt_dev->evbit);
	__set_bit(AM2120U_WT_PTT_KEY_OPEN,  kpd_am2120u_wt_dev->keybit);
	__set_bit(AM2120U_WT_PTT_KEY_CLOSE, kpd_am2120u_wt_dev->keybit);

	__set_bit(EV_SW, kpd_am2120u_wt_dev->evbit);
	__set_bit(SW_LID, kpd_am2120u_wt_dev->swbit);

	kpd_am2120u_wt_dev->id.bustype = BUS_HOST;
	kpd_am2120u_wt_dev->name = "ptt";
	if(input_register_device(kpd_am2120u_wt_dev))
	{
		AM2120U_WT_LOG("kpd_am2120u_wt_dev register : fail!\n");
	}
	else
	{
		AM2120U_WT_LOG("kpd_am2120u_wt_dev register : success!!\n");
	} 
	return 1;
}


 void kpd_send_key_am2120u_wt_ptt_open(void)
{
	if (kpd_am2120u_wt_dev==NULL) 
	{
		AM2120U_WT_ERR(" kpd_send_key_ptt_open Faill kpd_am2120u_wt_dev=NULL !!\n");
		return;
	}
		
	AM2120U_WT_LOG(" kpd_send_key_ptt_key_open!!\n");
	input_report_key(kpd_am2120u_wt_dev, AM2120U_WT_PTT_KEY_OPEN, 1);
	input_sync(kpd_am2120u_wt_dev);
	input_report_key(kpd_am2120u_wt_dev, AM2120U_WT_PTT_KEY_OPEN, 0);
	input_sync(kpd_am2120u_wt_dev);

}

 void kpd_send_key_am2120u_wt_ptt_close(void)
{

	if (kpd_am2120u_wt_dev==NULL) 
	{
		AM2120U_WT_ERR(" kpd_send_key_ptt_close Faill kpd_am2120u_wt_dev=NULL !!\n");
		return;
	}

	AM2120U_WT_LOG(" kpd_send_key_ptt_key_close!!\n");
	input_report_key(kpd_am2120u_wt_dev, AM2120U_WT_PTT_KEY_CLOSE, 1);
	input_sync(kpd_am2120u_wt_dev);
	input_report_key(kpd_am2120u_wt_dev, AM2120U_WT_PTT_KEY_CLOSE, 0);
	input_sync(kpd_am2120u_wt_dev);

 }
/**********************************************************************************************/
/* KPD END*/
/**********************************************************************************************/

/**********************************************************************************************/
/* GPIO BEGIN*/
/**********************************************************************************************/
struct pinctrl *am2120u_wt_pinctrl = NULL;
struct pinctrl_state *am2120u_wt_ptt_low = NULL;
struct pinctrl_state *am2120u_wt_ptt_high = NULL;
struct pinctrl_state *am2120u_wt_en_low = NULL;
struct pinctrl_state *am2120u_wt_en_high = NULL;
struct pinctrl_state *am2120u_wt_sq_eint_as_int = NULL;
struct pinctrl_state *am2120u_wt_ptt_eint_as_int = NULL;
struct pinctrl_state *am2120u_wt_ptt_eint_low = NULL;
struct pinctrl_state *am2120u_wt_ptt_eint_high = NULL;
struct pinctrl_state *am2120u_wt_spk_sw_low = NULL;
struct pinctrl_state *am2120u_wt_spk_sw_high = NULL;
struct pinctrl_state *am2120u_wt_extamp_pullhigh = NULL;
struct pinctrl_state *am2120u_wt_extamp_pulllow = NULL;

//static unsigned int AM2120U_WT_RX_PIN;
//static unsigned int AM2120U_WT_TX_PIN;
static unsigned int AM2120U_WT_PTT_PIN;
static unsigned int AM2120U_WT_EN_PIN;
static unsigned int AM2120U_WT_SQ_EINT_PIN;
static unsigned int AM2120U_WT_PTT_EINT_PIN;
static unsigned int AM2120U_WT_SPK_SW_PIN;

int am2120u_wt_gpio_init(struct platform_device *pdev)
{
	int ret = 0;
	AM2120U_WT_FUN(f);
	am2120u_wt_pinctrl = devm_pinctrl_get(&pdev->dev);
	if (IS_ERR(am2120u_wt_pinctrl)) {
		ret = PTR_ERR(am2120u_wt_pinctrl);
		AM2120U_WT_ERR("Cannot find am2120u_wt pinctrl!");
	}

	am2120u_wt_ptt_low = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_ptt_low");
	if (IS_ERR(am2120u_wt_ptt_low)) {
		ret = PTR_ERR(am2120u_wt_ptt_low);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_ptt_low\n");
	}

	am2120u_wt_ptt_high = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_ptt_high");
	if (IS_ERR(am2120u_wt_ptt_high)) {
		ret = PTR_ERR(am2120u_wt_ptt_high);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_ptt_high\n");
	}

	am2120u_wt_en_low = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_en_low");
	if (IS_ERR(am2120u_wt_en_low)) {
		ret = PTR_ERR(am2120u_wt_en_low);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_en_low\n");
	}

	am2120u_wt_en_high = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_en_high");
	if (IS_ERR(am2120u_wt_en_high)) {
		ret = PTR_ERR(am2120u_wt_en_high);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_en_high\n");
	}

	am2120u_wt_sq_eint_as_int = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_sq_eint_as_int");
	if (IS_ERR(am2120u_wt_sq_eint_as_int)) {
		ret = PTR_ERR(am2120u_wt_sq_eint_as_int);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_sq_eint_as_int\n");
	}

	am2120u_wt_ptt_eint_as_int = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_ptt_eint_as_int");
	if (IS_ERR(am2120u_wt_ptt_eint_as_int)) {
		ret = PTR_ERR(am2120u_wt_ptt_eint_as_int);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_ptt_eint_as_int\n");
	}

	am2120u_wt_ptt_eint_low = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_ptt_eint_low");
	if (IS_ERR(am2120u_wt_ptt_eint_low)) {
		ret = PTR_ERR(am2120u_wt_ptt_eint_low);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_ptt_eint_low\n");
	}
	am2120u_wt_ptt_eint_high = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_ptt_eint_high");
	if (IS_ERR(am2120u_wt_ptt_eint_high)) {
		ret = PTR_ERR(am2120u_wt_ptt_eint_high);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_ptt_eint_high\n");
	}

	am2120u_wt_spk_sw_low = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_spk_sw_low");
	if (IS_ERR(am2120u_wt_spk_sw_low)) {
		ret = PTR_ERR(am2120u_wt_spk_sw_low);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_spk_sw_low\n");
	}

	am2120u_wt_spk_sw_high = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_spk_sw_high");
	if (IS_ERR(am2120u_wt_spk_sw_high)) {
		ret = PTR_ERR(am2120u_wt_spk_sw_high);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_spk_sw_high\n");
	}
//OP ON HIGH
	am2120u_wt_extamp_pullhigh = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_extamp_pullhigh");
	if (IS_ERR(am2120u_wt_extamp_pullhigh)) {
		ret = PTR_ERR(am2120u_wt_extamp_pullhigh);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_extamp_pullhigh\n");
	}

//OP ON LOW
	am2120u_wt_extamp_pulllow = pinctrl_lookup_state(am2120u_wt_pinctrl, "am2120u_wt_extamp_pulllow");
	if (IS_ERR(am2120u_wt_extamp_pulllow)) {
		ret = PTR_ERR(am2120u_wt_extamp_pulllow);
		AM2120U_WT_ERR("pinctrl_init err, am2120u_wt_extamp_pulllow\n");
	}

	//AM2120U_WT_RX_PIN = of_get_named_gpio(pdev->dev.of_node, "am2120u_wt_rx", 0);
	//AM2120U_WT_TX_PIN = of_get_named_gpio(pdev->dev.of_node, "am2120u_wt_tx", 0);
	AM2120U_WT_PTT_PIN = of_get_named_gpio(pdev->dev.of_node, "am2120u_wt_ptt", 0);
	AM2120U_WT_EN_PIN = of_get_named_gpio(pdev->dev.of_node, "am2120u_wt_en", 0);
	AM2120U_WT_SQ_EINT_PIN = of_get_named_gpio(pdev->dev.of_node, "am2120u_wt_sq_eint", 0);
	AM2120U_WT_PTT_EINT_PIN = of_get_named_gpio(pdev->dev.of_node, "am2120u_wt_ptt_eint", 0);
	AM2120U_WT_SPK_SW_PIN = of_get_named_gpio(pdev->dev.of_node, "am2120u_wt_spk_sw", 0);

	//ret = gpio_request(AM2120U_WT_RX_PIN, "am2120u_wt_rx");
	//if (ret < 0)
	//	AM2120U_WT_ERR("Unable to request AM2120U_WT_RX_PIN\n");

	//ret = gpio_request(AM2120U_WT_TX_PIN, "am2120u_wt_tx");
	//if (ret < 0)
	//	AM2120U_WT_ERR("Unable to request AM2120U_WT_TX_PIN\n");

	ret = gpio_request(AM2120U_WT_PTT_PIN, "am2120u_wt_ptt");
	if (ret < 0)
		AM2120U_WT_ERR("Unable to request AM2120U_WT_PTT_PIN\n");

	ret = gpio_request(AM2120U_WT_EN_PIN, "am2120u_wt_en");
	if (ret < 0)
		AM2120U_WT_ERR("Unable to request AM2120U_WT_EN_PIN\n");

	ret = gpio_request(AM2120U_WT_SQ_EINT_PIN, "am2120u_wt_sq_eint");
	if (ret < 0)
		AM2120U_WT_ERR("Unable to request AM2120U_WT_SQ_EINT_PIN\n");

	ret = gpio_request(AM2120U_WT_PTT_EINT_PIN, "am2120u_wt_ptt_eint");
	if (ret < 0)
		AM2120U_WT_ERR("Unable to request AM2120U_WT_PTT_EINT_PIN\n");

	ret = gpio_request(AM2120U_WT_SPK_SW_PIN, "am2120u_wt_spk_sw");
	if (ret < 0)
		AM2120U_WT_ERR("Unable to request AM2120U_WT_SPK_SW_PIN\n");

	//gpio_direction_output(AM2120U_WT_PTT_PIN,0);
	//gpio_direction_output(AM2120U_WT_EN_PIN,1);
	//gpio_direction_output(AM2120U_WT_SPK_SW_PIN,1);
	//gpio_direction_input(AM2120U_WT_SQ_EINT_PIN);
	//AM2120U_WT_LOG("AM2120U_WT_SQ_EINT_PIN=%d", gpio_get_value(AM2120U_WT_SQ_EINT_PIN));

//defaut
	pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_en_low);    //powerdown//high for test
	mdelay(500);
	am2120u_wt_is_opening_flag=0; //default=0; 1 for test

	pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_high);  //receive
	pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low);//phone
	pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_eint_as_int);
	pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_sq_eint_as_int);

	return ret;
}

static int am2120u_wt_gpio_remove(struct platform_device *pdev)
{
	AM2120U_WT_FUN(f);
	//gpio_free(AM2120U_WT_RX_PIN);
	//gpio_free(AM2120U_WT_TX_PIN);
	gpio_free(AM2120U_WT_PTT_PIN);
	gpio_free(AM2120U_WT_EN_PIN);
	gpio_free(AM2120U_WT_SQ_EINT_PIN);
	gpio_free(AM2120U_WT_PTT_EINT_PIN);
	gpio_free(AM2120U_WT_SPK_SW_PIN);

	return 0;
}
/**********************************************************************************************/
/* GPIO END*/
/**********************************************************************************************/

/**********************************************************************************************/
/* PTT EINT BEGIN*/
/**********************************************************************************************/
void am2120u_wt_ptt_eint_do_work(struct work_struct *work)
{
	AM2120U_WT_FUN(f);
	wake_lock_timeout(&am2120u_wt_ptt_eint_lock, 2*HZ);////系统从深度休眠中唤醒并保证系统wakup一段时间后执行

	if( cur_am2120u_wt_ptt_eint_state ==OPEN)  
	{
		AM2120U_WT_LOG("****Open && RECEIVE******** \n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_high); //receive
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low); //phone //if recieve sq to change
		kpd_send_key_am2120u_wt_ptt_open();

	}		
	else
	{
		AM2120U_WT_LOG("****Close && SEND******** \n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_low); //send

		if(1==am2120u_wt_is_opening_flag)//moudle opened
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_high); //moudle
		else
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low); //phone

		kpd_send_key_am2120u_wt_ptt_close();
	}

	msleep(100);//100ms 
	am2120u_wt_ptt_eint_irq_flag=0;
}

static irqreturn_t am2120u_wt_ptt_eint_interrupt_handler(int irq, void *dev_id)
{
	int ret=0;
	AM2120U_WT_LOG("am2120u_wt_ptt interrupt has been triggered\n");
	am2120u_wt_ptt_eint_irq_flag=1;

	if (cur_am2120u_wt_ptt_eint_gpio_state)
	{
		cur_am2120u_wt_ptt_eint_gpio_state=0;
		irq_set_irq_type(am2120u_wt_ptt_eint_irq,IRQ_TYPE_LEVEL_HIGH);
	}
	else
	{
		cur_am2120u_wt_ptt_eint_gpio_state=1;
		irq_set_irq_type(am2120u_wt_ptt_eint_irq,IRQ_TYPE_LEVEL_LOW);
	}

	cur_am2120u_wt_ptt_eint_state = cur_am2120u_wt_ptt_eint_gpio_state;

	ret = queue_work(am2120u_wt_ptt_eint_workqueue, &am2120u_wt_ptt_eint_work); //调度执行，不一定立刻执行，取决于目前CPU情况
	return IRQ_HANDLED;
}

static int am2120u_wt_ptt_eint_irq_registration(void)
{
	struct device_node *node = NULL;
	int ret = 0;
	u32 ints[2] = { 0, 0 };
	AM2120U_WT_FUN(f);

	cur_am2120u_wt_ptt_eint_gpio_state = gpio_get_value(AM2120U_WT_PTT_EINT_PIN);
	AM2120U_WT_LOG("AM2120U_WT_PTT_EINT_PIN:%d\n", cur_am2120u_wt_ptt_eint_gpio_state);
	cur_am2120u_wt_ptt_eint_state = cur_am2120u_wt_ptt_eint_gpio_state;

	wake_lock_timeout(&am2120u_wt_ptt_eint_lock, 2*HZ);//系统从深度休眠中唤醒并保证系统wakup一段时间后执行

	if( cur_am2120u_wt_ptt_eint_state ==OPEN)  
	{
		AM2120U_WT_LOG("****Open******** \n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_high);
	}		
	else
	{
		AM2120U_WT_LOG("****Close******** \n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_low);
	}

	node = of_find_matching_node(node, am2120u_wt_ptt_eint_of_match);
	if (node) {
		of_property_read_u32_array(node, "debounce", ints, ARRAY_SIZE(ints));
		gpio_set_debounce(ints[0], ints[1]);

		am2120u_wt_ptt_eint_irq = irq_of_parse_and_map(node, 0);
		ret = request_irq(am2120u_wt_ptt_eint_irq, (irq_handler_t) am2120u_wt_ptt_eint_interrupt_handler,IRQF_TRIGGER_FALLING,"mediatek, PTT_EINT-eint", NULL);
			if (ret) {
				ret = -1;
				AM2120U_WT_ERR("am2120u_wt_ptt request_irq IRQ LINE NOT AVAILABLE!.");
			}
		}
	else {
		AM2120U_WT_ERR("am2120u_wt_ptt request_irq can not find am2120u_wt_ptt eint device node!.");
		ret = -1;
	}
	AM2120U_WT_LOG("am2120u_wt_ptt_eint_irq:%d, debounce:%d-%d\n", am2120u_wt_ptt_eint_irq, ints[0], ints[1]);

	if (cur_am2120u_wt_ptt_eint_gpio_state)
	{
		irq_set_irq_type(am2120u_wt_ptt_eint_irq,IRQ_TYPE_LEVEL_LOW);
	}
	else{
		irq_set_irq_type(am2120u_wt_ptt_eint_irq,IRQ_TYPE_LEVEL_HIGH);
	}

	return ret;
}
/**********************************************************************************************/
/* PTT EINT END*/
/**********************************************************************************************/
/**********************************************************************************************/
/* SQ EINT BEGIN*/
/**********************************************************************************************/
void am2120u_wt_sq_eint_do_work(struct work_struct *work)
{
	AM2120U_WT_FUN(f);
	wake_lock_timeout(&am2120u_wt_sq_eint_lock, 2*HZ);////系统从深度休眠中唤醒并保证系统wakup一段时间后执行

	if( cur_am2120u_wt_sq_eint_state ==OPEN)  
	{
		AM2120U_WT_LOG("****Open && NO RECEIVE******** \n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low); //phone
		//pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		mdelay(10);
		kpd_send_key_am2120u_wt_ptt_open();

	}		
	else
	{
		AM2120U_WT_LOG("****Close && RECEIVE******** \n");
		if(1==am2120u_wt_is_opening_flag){//moudle opened
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_high); //moudle
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		mdelay(10);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		mdelay(10);
		}
		else
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low); //phone
		kpd_send_key_am2120u_wt_ptt_close();
	}

	msleep(100);//100ms 
	am2120u_wt_sq_eint_irq_flag=0;
}

static irqreturn_t am2120u_wt_sq_eint_interrupt_handler(int irq, void *dev_id)
{
	int ret=0;
	AM2120U_WT_LOG("am2120u_wt_sq interrupt has been triggered\n");
	am2120u_wt_sq_eint_irq_flag=1;

	if (cur_am2120u_wt_sq_eint_gpio_state)
	{
		cur_am2120u_wt_sq_eint_gpio_state=0;
		irq_set_irq_type(am2120u_wt_sq_eint_irq,IRQ_TYPE_LEVEL_HIGH);
	}
	else
	{
		cur_am2120u_wt_sq_eint_gpio_state=1;
		irq_set_irq_type(am2120u_wt_sq_eint_irq,IRQ_TYPE_LEVEL_LOW);
	}

	cur_am2120u_wt_sq_eint_state = cur_am2120u_wt_sq_eint_gpio_state;

	ret = queue_work(am2120u_wt_sq_eint_workqueue, &am2120u_wt_sq_eint_work); //调度执行，不一定立刻执行，取决于目前CPU情况
	return IRQ_HANDLED;
}

static int am2120u_wt_sq_eint_irq_registration(void)
{
	struct device_node *node = NULL;
	int ret = 0;
	u32 ints[2] = { 0, 0 };
	AM2120U_WT_FUN(f);

	cur_am2120u_wt_sq_eint_gpio_state = gpio_get_value(AM2120U_WT_SQ_EINT_PIN);
	AM2120U_WT_LOG("AM2120U_WT_SQ_EINT_PIN:%d\n", cur_am2120u_wt_sq_eint_gpio_state);
	cur_am2120u_wt_sq_eint_state = cur_am2120u_wt_sq_eint_gpio_state;

	wake_lock_timeout(&am2120u_wt_sq_eint_lock, 2*HZ);//系统从深度休眠中唤醒并保证系统wakup一段时间后执行

	if( cur_am2120u_wt_sq_eint_state ==OPEN)  
	{
		AM2120U_WT_LOG("****Open && NO RECEIVE******** \n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low); //phone
		//pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		mdelay(10);
	}		
	else
	{
		AM2120U_WT_LOG("****Close&&  RECEIVE******** \n");
		if(1==am2120u_wt_is_opening_flag){//moudle opened
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_high); //moudle
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		mdelay(10);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pulllow);
		udelay(2);
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_extamp_pullhigh); 
		mdelay(10);
		}
		else
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low); //phone
	}

	node = of_find_matching_node(node, am2120u_wt_sq_eint_of_match);
	if (node) {
		of_property_read_u32_array(node, "debounce", ints, ARRAY_SIZE(ints));
		gpio_set_debounce(ints[0], ints[1]);

		am2120u_wt_sq_eint_irq = irq_of_parse_and_map(node, 0);
		ret = request_irq(am2120u_wt_sq_eint_irq, (irq_handler_t) am2120u_wt_sq_eint_interrupt_handler,IRQF_TRIGGER_FALLING,"mediatek, AM2120U_WT_SQ_EINT-eint", NULL);
			if (ret) {
				ret = -1;
				AM2120U_WT_ERR("am2120u_wt_sq request_irq IRQ LINE NOT AVAILABLE!.");
			}
		}
	else {
		AM2120U_WT_ERR("am2120u_wt_sq request_irq can not find am2120u_wt_sq eint device node!.");
		ret = -1;
	}
	AM2120U_WT_LOG("am2120u_wt_sq_eint_irq:%d, debounce:%d-%d\n", am2120u_wt_sq_eint_irq, ints[0], ints[1]);

	if (cur_am2120u_wt_sq_eint_gpio_state)
	{
		irq_set_irq_type(am2120u_wt_sq_eint_irq,IRQ_TYPE_LEVEL_LOW);
	}
	else{
		irq_set_irq_type(am2120u_wt_sq_eint_irq,IRQ_TYPE_LEVEL_HIGH);
	}

	return ret;
}
/**********************************************************************************************/
/* SQ EINT END*/
/**********************************************************************************************/

/**********************************************************************************************/
/* file_operations BEGIN*/
/**********************************************************************************************/
static int am2120u_wt_open(struct inode *inode, struct file *file)
{
	AM2120U_WT_FUN(f);
	return 0;	
}

static int am2120u_wt_release(struct inode *inode, struct file *file)
{
	AM2120U_WT_FUN(f);
	return 0;
}

static ssize_t am2120u_wt_write(struct file *file, const char __user *buf, size_t count, loff_t * ppos)
{
	unsigned long missing;
	char inbuffer[4]={0};
	int i;
	AM2120U_WT_FUN(f);

	if(count>4)
	{
		count=4;
	}
	missing = copy_from_user(inbuffer, buf, count);
	AM2120U_WT_LOG("am2120u_wt_copy_from_user_missing=%ld **\n",missing);

	for(i=0;i<count;i++)
	{
		AM2120U_WT_LOG("am2120u_wt_write[%d]=0x%x **\n",i,inbuffer[i]);
	} 

	if(inbuffer[0]==0x43)//C //POWER ON
	{
		AM2120U_WT_LOG("am2120u_wt_write 0x43 to set en high to power on**\n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_en_high);
		mdelay(500);
		am2120u_wt_is_opening_flag=1;

	}else if(inbuffer[0]==0x44)//D //POWER DOWN
	{
		AM2120U_WT_LOG("am2120u_wt_write 0x44 to set en low to power down**\n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_en_low);
		mdelay(50);
		am2120u_wt_is_opening_flag=0;
	}
	#if 0 //for test
	else if(inbuffer[0]==0x45)//E
	{
		AM2120U_WT_LOG("am2120u_wt_write 0x45 to set ptt high to receive**\n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_high);
		mdelay(5);
	}
	else if(inbuffer[0]==0x46)//F
	{
		AM2120U_WT_LOG("am2120u_wt_write 0x46 to set ptt low to send**\n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_ptt_low);
		mdelay(5);
	}
	else if(inbuffer[0]==0x47)//G
	{
		AM2120U_WT_LOG("am2120u_wt_write 0x45 to set spk_sw high to moudle**\n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_high);
		mdelay(5);
	}
	else if(inbuffer[0]==0x48)//H
	{
		AM2120U_WT_LOG("am2120u_wt_write 0x46 to set spk_sw low to phone**\n");
		pinctrl_select_state(am2120u_wt_pinctrl, am2120u_wt_spk_sw_low);
		mdelay(5);
	}
	#endif
	else
	{
		AM2120U_WT_LOG("am2120u_wt_write no command**\n");
	}
	
	return count; 	
}

/***********************************************************************************************/
static ssize_t  am2120u_wt_read(struct file *file, char __user *buf, size_t size, loff_t *ppos)
{
	char sdas[4]={0,0,0,0};
	ssize_t       status = 1;
	unsigned long missing;

	AM2120U_WT_FUN(f);

	sdas[0]=gpio_get_value(AM2120U_WT_PTT_EINT_PIN);
	sdas[1]=gpio_get_value(AM2120U_WT_SQ_EINT_PIN);

	missing = copy_to_user(buf, sdas, status);

	if (missing == status)
	{
		status = -EFAULT;
	}
	else
	{
		status = status - missing;
	}
 
	AM2120U_WT_LOG("am2120u_wt_sdas =0x%x,0x%x\n",sdas[0],sdas[1]);

	return status;
}

static struct file_operations am2120u_wt_fops = {
	.owner   =  THIS_MODULE,    
	.open    =  am2120u_wt_open,  
	.release =  am2120u_wt_release,	   
	.read	 =  am2120u_wt_read,
	.write	 =  am2120u_wt_write,
};
/**********************************************************************************************/
/* file_operations END*/
/**********************************************************************************************/

static int am2120u_wt_probe(struct platform_device *pdev)
{   
	int ret = 0;
    
	AM2120U_WT_FUN(f); 
	/*注册字符设备*/
	am2120u_wt_major = register_chrdev(0,AM2120U_WT_NAME,&am2120u_wt_fops);
    	if (am2120u_wt_major < 0) {
		AM2120U_WT_ERR("register_chrdev failed=%d\n",  am2120u_wt_major);
		goto am2120u_wt_probe_error;
	}

	/*以下两点为用户层的udev服务，提供可用接口*/
	/*创建一个类，存放于sysfs，在sys/class/目录*/
	am2120u_wt_class = class_create(THIS_MODULE, AM2120U_WT_NAME);  //sys/class
    	if(NULL == am2120u_wt_class){
		 AM2120U_WT_ERR("class_create fail\n");
       		 goto am2120u_wt_probe_error;
    	}

	/*创建设备节点，在DEV目录，加载模块的时候，用户空间中的udev会自动响应，去sysfs下寻找对应的类从而创建设备节点*/
	am2120u_wt_device = device_create(am2120u_wt_class,NULL,MKDEV(am2120u_wt_major,0),NULL,AM2120U_WT_NAME); //dev/
    	if(NULL == am2120u_wt_device){
		 AM2120U_WT_ERR("device_create fail\n");
       		 goto am2120u_wt_probe_error;
    	}
	/*按键初始化*/
	am2120u_wt_ptt_kpd_input_init();

	/*GPIO初始化*/
	ret = am2120u_wt_gpio_init(pdev);
	if (ret < 0)
	{
		 AM2120U_WT_ERR("gpio_init fail\n");
	}

	/*工作队列*/
	wake_lock_init(&am2120u_wt_ptt_eint_lock, WAKE_LOCK_SUSPEND, "am2120u_wt_ptt_eint wakelock");//LOCK
	am2120u_wt_ptt_eint_workqueue = create_singlethread_workqueue("am2120u_wt_ptt_eint");//创建一个单线程的工作队列
	INIT_WORK(&am2120u_wt_ptt_eint_work, am2120u_wt_ptt_eint_do_work);

	wake_lock_init(&am2120u_wt_sq_eint_lock, WAKE_LOCK_SUSPEND, "am2120u_wt_sq_eint wakelock");//LOCK
	am2120u_wt_sq_eint_workqueue = create_singlethread_workqueue("am2120u_wt_sq_eint");//创建一个单线程的工作队列
	INIT_WORK(&am2120u_wt_sq_eint_work, am2120u_wt_sq_eint_do_work);

	/*中断初始化*/
	ret = am2120u_wt_ptt_eint_irq_registration();
	if (ret < 0) {
		AM2120U_WT_ERR("Failed to am2120u_wt_ptt_irq_registration");
		return ret;
	}else{
		AM2120U_WT_LOG("Success to am2120u_wt_ptt_irq_registration");
	}

	ret = am2120u_wt_sq_eint_irq_registration();
	if (ret < 0) {
		AM2120U_WT_ERR("Failed to am2120u_wt_sq_irq_registration");
		return ret;
	}else{
		AM2120U_WT_LOG("Success to am2120u_wt_sq_irq_registration");
	}

	return 0;

am2120u_wt_probe_error:
	device_destroy(am2120u_wt_class,MKDEV(am2120u_wt_major,0));  
	class_destroy(am2120u_wt_class); 
	unregister_chrdev(am2120u_wt_major,AM2120U_WT_NAME);
	return -1;
}

static int am2120u_wt_remove(struct platform_device *pdev)
{	
	AM2120U_WT_FUN(f); 
	am2120u_wt_gpio_remove(pdev);
	return 0;
}

struct platform_driver am2120u_wt_drv = {
	.probe		= am2120u_wt_probe,
	.remove		= am2120u_wt_remove,
	.driver		= {
		.name	= AM2120U_WT_NAME,
		.owner = THIS_MODULE,
		.of_match_table = am2120u_wt_of_match,
	}
};

static int am2120u_wt_init(void)
{
	int ret;
	AM2120U_WT_FUN(f);

	ret = platform_driver_register(&am2120u_wt_drv);

	if (ret)
	{
		AM2120U_WT_ERR("am2120u_wt_platform_driver_register failed\n");
		return ret;
	}
	return 0;
}

static void am2120u_wt_exit(void)
{
	AM2120U_WT_FUN(f);
	platform_driver_unregister(&am2120u_wt_drv);
}

module_init(am2120u_wt_init);
module_exit(am2120u_wt_exit);

MODULE_AUTHOR("Crystal Shen, <crystal.shen@163.com>");
MODULE_DESCRIPTION("Just For am2120u_wt_bird");
MODULE_VERSION("2017-07-06-V01");
MODULE_LICENSE("GPL");

