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

#ifdef CONFIG_BIRD_CARDREADER_SUPPORT_FOR_MX2150P_V01_UART
#include <upmu_common.h>
#endif

#include "cardreader_bird.h"

/**********************************************************************************************/
/* configuration*/
/**********************************************************************************************/
#define CARDREADER_NAME	"cardreader"

#define OPEN      (1)
#define CLOSE     (0)

#define CARDREADER_CLOSE      KEY_F8
#define CARDREADER_OPEN       KEY_F9

static int cur_cardreader_sim_eint_state = OPEN;
static int cur_eint_gpio_state =0 ;

static int cardreader_major;
static struct class *cardreader_class = NULL;
static struct device *cardreader_device = NULL;

unsigned int cardreader_irq = 0;
static int cardreader_irq_flag=0;

/*工作队列*/
struct wake_lock cardreader_sim_eint_lock;
static struct work_struct cardreader_sim_eint_work;
static struct workqueue_struct * cardreader_sim_eint_workqueue = NULL;

static const struct of_device_id cardreader_of_match[] = {
	{
		.compatible = "mediatek,cardreader"
	},
	{},
};
static const struct of_device_id cardreader_sim_eint_of_match[] = {
	{
		.compatible = "mediatek,sim_eint"
	},
	{},
};

/**********************************************************************************************/
/* GPIO BEGIN*/
/**********************************************************************************************/
struct pinctrl *cardreader_pinctrl = NULL;
struct pinctrl_state *cardreader_default  = NULL;
struct pinctrl_state *cardreader_usb_sw_high = NULL;
struct pinctrl_state *cardreader_usb_sw_low = NULL;
struct pinctrl_state *cardreader_sim_eint_as_int = NULL;
struct pinctrl_state *cardreader_sim_eint_output0 = NULL;
struct pinctrl_state *cardreader_sim_eint_output1 = NULL;

unsigned int cardreader_usb_sw;
unsigned int CARDREADER_SIM_EINT_PIN;

int cardreader_gpio_init(struct platform_device *pdev)
{
	int ret = 0;

	cardreader_pinctrl = devm_pinctrl_get(&pdev->dev);
	if (IS_ERR(cardreader_pinctrl)) {
		ret = PTR_ERR(cardreader_pinctrl);
		CARDREADER_ERR("Cannot find cardreader pinctrl!");
	}

	cardreader_default = pinctrl_lookup_state(cardreader_pinctrl, "default");
	if (IS_ERR(cardreader_default)) {
		ret = PTR_ERR(cardreader_default);
		CARDREADER_ERR("Cannot find find touch pinctrl cardreader_default\n");
	}

	cardreader_usb_sw_high = pinctrl_lookup_state(cardreader_pinctrl, "cardreader_usb_sw_high");
	if (IS_ERR(cardreader_usb_sw_high)) {
		ret = PTR_ERR(cardreader_usb_sw_high);
		CARDREADER_ERR("pinctrl_init err, cardreader_usb_sw_high\n");
	}

	cardreader_usb_sw_low = pinctrl_lookup_state(cardreader_pinctrl, "cardreader_usb_sw_low");
	if (IS_ERR(cardreader_usb_sw_low)) {
		ret = PTR_ERR(cardreader_usb_sw_low);
		CARDREADER_ERR("pinctrl_init err, cardreader_usb_sw_low\n");
	}

	cardreader_sim_eint_as_int = pinctrl_lookup_state(cardreader_pinctrl, "cardreader_sim_eint_as_int");
	if (IS_ERR(cardreader_sim_eint_as_int)) {
		ret = PTR_ERR(cardreader_sim_eint_as_int);
		CARDREADER_ERR("pinctrl_init err, cardreader_sim_eint_as_int\n");
	}

	cardreader_sim_eint_output0 = pinctrl_lookup_state(cardreader_pinctrl, "cardreader_sim_eint_output0");
	if (IS_ERR(cardreader_sim_eint_output0)) {
		ret = PTR_ERR(cardreader_sim_eint_output0);
		CARDREADER_ERR("pinctrl_init err, cardreader_sim_eint_output0\n");
	}

	cardreader_sim_eint_output1 = pinctrl_lookup_state(cardreader_pinctrl, "cardreader_sim_eint_output1");
	if (IS_ERR(cardreader_sim_eint_output1)) {
		ret = PTR_ERR(cardreader_sim_eint_output1);
		CARDREADER_ERR("pinctrl_init err, cardreader_sim_eint_output1\n");
	}

	pinctrl_select_state(cardreader_pinctrl, cardreader_sim_eint_as_int);//set eint as int
	pinctrl_select_state(cardreader_pinctrl, cardreader_usb_sw_low);

#ifdef CONFIG_BIRD_CARDREADER_SUPPORT_FOR_MX2150P_V01_UART
	pmic_set_register_value(PMIC_RG_VGP1_EN,0);
#endif

	cardreader_usb_sw = of_get_named_gpio(pdev->dev.of_node, "cardreader_usb_sw", 0);
	CARDREADER_SIM_EINT_PIN = of_get_named_gpio(pdev->dev.of_node, "cardreader_sim_eint", 0);

	ret = gpio_request(cardreader_usb_sw, "cardreader_usb_sw");
	if (ret < 0)
		CARDREADER_ERR("Unable to request cardreader_usb_sw\n");

	ret = gpio_request(CARDREADER_SIM_EINT_PIN, "cardreader_sim_eint");
	if (ret < 0)
		CARDREADER_ERR("Unable to request CARDREADER_SIM_EINT_PIN\n");;

	//gpio_direction_output(cardreader_usb_sw,1);
	//gpio_direction_input(CARDREADER_SIM_EINT_PIN);

	return ret;
}

static int cardreader_gpio_remove(struct platform_device *pdev)
{
	CARDREADER_FUN(f);

	gpio_free(cardreader_usb_sw);
	gpio_free(CARDREADER_SIM_EINT_PIN);
	return 0;
}

/**********************************************************************************************/
/* GPIO END*/
/**********************************************************************************************/

/**********************************************************************************************/
/* EINT BEGIN*/
/**********************************************************************************************/
void cardreader_sim_eint_do_work(struct work_struct *work)
{
	CARDREADER_FUN(f);
	wake_lock_timeout(&cardreader_sim_eint_lock, 2*HZ);////系统从深度休眠中唤醒并保证系统wakup一段时间后执行

	if( cur_cardreader_sim_eint_state ==OPEN)  
	{
		CARDREADER_LOG("****Open******** \n");
		pinctrl_select_state(cardreader_pinctrl, cardreader_usb_sw_low);

#ifdef CONFIG_BIRD_CARDREADER_SUPPORT_FOR_MX2150P_V01_UART
		pmic_set_register_value(PMIC_RG_VGP1_EN,0);
#endif

	}		
	else
	{
		CARDREADER_LOG("****Close******** \n");
		pinctrl_select_state(cardreader_pinctrl, cardreader_usb_sw_high);

#ifdef CONFIG_BIRD_CARDREADER_SUPPORT_FOR_MX2150P_V01_UART
		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x07);
#endif
	}

	msleep(100);//100ms 
	cardreader_irq_flag=0;
}

static irqreturn_t cardreader_sim_eint_interrupt_handler(int irq, void *dev_id)
{
	int ret=0;
	CARDREADER_LOG("cardreader interrupt has been triggered\n");
	cardreader_irq_flag=1;

	if (cur_eint_gpio_state)
	{
		cur_eint_gpio_state=0;
		irq_set_irq_type(cardreader_irq,IRQ_TYPE_LEVEL_HIGH);
	}
	else
	{
		cur_eint_gpio_state=1;
		irq_set_irq_type(cardreader_irq,IRQ_TYPE_LEVEL_LOW);
	}

	cur_cardreader_sim_eint_state = cur_eint_gpio_state;

	ret = queue_work(cardreader_sim_eint_workqueue, &cardreader_sim_eint_work); //调度执行，不一定立刻执行，取决于目前CPU情况
	return IRQ_HANDLED;
}

static int cardreader_irq_registration(void)
{
	struct device_node *node = NULL;
	int ret = 0;
	u32 ints[2] = { 0, 0 };
	CARDREADER_FUN(f);

	cur_eint_gpio_state = gpio_get_value(CARDREADER_SIM_EINT_PIN);
	CARDREADER_LOG("CARDREADER_SIM_EINT_PIN:%d\n", cur_eint_gpio_state);
	cur_cardreader_sim_eint_state = cur_eint_gpio_state;

	wake_lock_timeout(&cardreader_sim_eint_lock, 2*HZ);//系统从深度休眠中唤醒并保证系统wakup一段时间后执行

	if( cur_cardreader_sim_eint_state ==OPEN)  
	{
		CARDREADER_LOG("****Open******** \n");
		pinctrl_select_state(cardreader_pinctrl, cardreader_usb_sw_low);

#ifdef CONFIG_BIRD_CARDREADER_SUPPORT_FOR_MX2150P_V01_UART
		pmic_set_register_value(PMIC_RG_VGP1_EN,0);
#endif
	}		
	else
	{
		CARDREADER_LOG("****Close******** \n");
		pinctrl_select_state(cardreader_pinctrl, cardreader_usb_sw_high);

#ifdef CONFIG_BIRD_CARDREADER_SUPPORT_FOR_MX2150P_V01_UART
		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x07);
#endif
	}

	node = of_find_matching_node(node, cardreader_sim_eint_of_match);
	if (node) {
		of_property_read_u32_array(node, "debounce", ints, ARRAY_SIZE(ints));
		gpio_set_debounce(ints[0], ints[1]);

		cardreader_irq = irq_of_parse_and_map(node, 0);
		ret = request_irq(cardreader_irq, (irq_handler_t) cardreader_sim_eint_interrupt_handler,IRQF_TRIGGER_FALLING,"mediatek, CARDREADER_EINT-eint", NULL);
			if (ret) {
				ret = -1;
				CARDREADER_ERR("cardreader request_irq IRQ LINE NOT AVAILABLE!.");
			}
		}
	else {
		CARDREADER_ERR("cardreader request_irq can not find cardreader eint device node!.");
		ret = -1;
	}
	CARDREADER_LOG("cardreader_irq:%d, debounce:%d-%d\n", cardreader_irq, ints[0], ints[1]);

	if (cur_eint_gpio_state)
	{
		irq_set_irq_type(cardreader_irq,IRQ_TYPE_LEVEL_LOW);
	}
	else{
		irq_set_irq_type(cardreader_irq,IRQ_TYPE_LEVEL_HIGH);
	}

	return ret;
}
/**********************************************************************************************/
/* EINT END*/
/**********************************************************************************************/

/**********************************************************************************************/
/* file_operations BEGIN*/
/**********************************************************************************************/
static int cardreader_open(struct inode *inode, struct file *file)
{
	CARDREADER_FUN(f);
	return 0;	
}

static int cardreader_release(struct inode *inode, struct file *file)
{
	CARDREADER_FUN(f);
	return 0;
}

static ssize_t cardreader_write(struct file *file, const char __user *buf, size_t count, loff_t * ppos)
{
	unsigned long missing;
	char inbuffer[4]={0};
	int i;
	CARDREADER_FUN(f);

	if(count>4)
	{
		count=4;
	}
	missing = copy_from_user(inbuffer, buf, count);
	CARDREADER_LOG("cardreader_copy_from_user_missing=%ld **\n",missing);

	for(i=0;i<count;i++)
	{
		CARDREADER_LOG("cardreader_write[%d]=0x%x **\n",i,inbuffer[i]);
	} 

	if(inbuffer[0]==0x43)//C
	{
		CARDREADER_LOG("OPEN CHONGDIANBAO,inbuffer[0]=%x\n",inbuffer[0]);
		if (gpio_get_value(CARDREADER_SIM_EINT_PIN)){
			CARDREADER_LOG("CARDREADER_SIM_EINT_PIN=high\n");
		}
		else{
		}	
	}
	else if(inbuffer[0]==0x44)//D
	{
		CARDREADER_LOG("CLOSE CHONGDIANBAO,inbuffer[0]=%x\n",inbuffer[0]);
		if (gpio_get_value(CARDREADER_SIM_EINT_PIN)){
			CARDREADER_LOG("CARDREADER_SIM_EINT_PIN=high\n");
		}
		else{
		}
	}
	CARDREADER_LOG("cardreader_inbuffer[0]=0x%d\n",inbuffer[0]);

	return count; 	
}

static ssize_t cardreader_read(struct file *file, char __user *buf, size_t size, loff_t *ppos)
{
	char sdas[4]={0,0,0,0};
	ssize_t       status = 1;
	unsigned long missing;

	CARDREADER_FUN(f);

	sdas[0]=gpio_get_value(CARDREADER_SIM_EINT_PIN);

	missing = copy_to_user(buf, sdas, status);

	if (missing == status)
	{
		status = -EFAULT;
	}
	else
	{
		status = status - missing;
	}
 
	CARDREADER_LOG("cardreader_sdas =0x%x\n",sdas[0]);

	return status;	

}

static struct file_operations cardreader_fops = {
	.owner   =  THIS_MODULE,    
	.open    =  cardreader_open,
	.read	 =  cardreader_read,
	.write	 =  cardreader_write,  
	.release =  cardreader_release,	   
};
/**********************************************************************************************/
/* file_operations END*/
/**********************************************************************************************/


static int cardreader_probe(struct platform_device *pdev)
{  
	int ret;   
	CARDREADER_FUN(f); 
	/*注册字符设备*/
	cardreader_major = register_chrdev(0,CARDREADER_NAME,&cardreader_fops);
    	if (cardreader_major < 0) {
		CARDREADER_ERR("register_chrdev failed=%d\n",  cardreader_major);
		goto cardreader_probe_error;
	}

	/*以下两点为用户层的udev服务，提供可用接口*/
	/*创建一个类，存放于sysfs，在sys/class/目录*/
	cardreader_class = class_create(THIS_MODULE, CARDREADER_NAME);  //sys/class
    	if(NULL == cardreader_class){
		 CARDREADER_ERR("class_create fail\n");
       		 goto cardreader_probe_error;
    	}
	
	/*创建设备节点，在DEV目录，加载模块的时候，用户空间中的udev会自动响应，去sysfs下寻找对应的类从而创建设备节点*/
	cardreader_device = device_create(cardreader_class,NULL,MKDEV(cardreader_major,0),NULL,CARDREADER_NAME); //dev/
    	if(NULL == cardreader_device){
		 CARDREADER_ERR("device_create fail\n");
       		 goto cardreader_probe_error;
    	}
	
	/*GPIO初始化*/
	cardreader_gpio_init(pdev);

	/*工作队列*/
	wake_lock_init(&cardreader_sim_eint_lock, WAKE_LOCK_SUSPEND, "cardreader_ein wakelock");//LOCK
	cardreader_sim_eint_workqueue = create_singlethread_workqueue("cardreader_sim_eint");//创建一个单线程的工作队列
	INIT_WORK(&cardreader_sim_eint_work, cardreader_sim_eint_do_work);

	/*中断初始化*/
	ret = cardreader_irq_registration();
	if (ret < 0) {
		CARDREADER_ERR("Failed to cardreader_irq_registration");
		return ret;
	}else{
		CARDREADER_LOG("Success to cardreader_irq_registration");
	}

	return 0;

cardreader_probe_error:
	device_destroy(cardreader_class,MKDEV(cardreader_major,0));  
	class_destroy(cardreader_class); 
	unregister_chrdev(cardreader_major,CARDREADER_NAME);
	return -1;
}

static int cardreader_remove(struct platform_device *pdev)
{	
	CARDREADER_FUN(f); 
	destroy_workqueue(cardreader_sim_eint_workqueue);
	cardreader_gpio_remove(pdev);
	return 0;
}

struct platform_driver cardreader_drv = {
	.probe		= cardreader_probe,
	.remove		= cardreader_remove,
	.driver		= {
		.name	= CARDREADER_NAME,
		.owner = THIS_MODULE,
		.of_match_table = cardreader_of_match,
	}
};

static int cardreader_init(void)
{
	int ret;
	CARDREADER_FUN(f);

	ret = platform_driver_register(&cardreader_drv);

	if (ret)
	{
		CARDREADER_ERR("cardreader_platform_driver_register failed\n");
		return ret;
	}
	return 0;
}

static void cardreader_exit(void)
{
	CARDREADER_FUN(f);
	platform_driver_unregister(&cardreader_drv);
}

module_init(cardreader_init);
module_exit(cardreader_exit);

MODULE_AUTHOR("Crystal Shen, <crystal.shen@163.com>");
MODULE_DESCRIPTION("Just For CARDREADER");
MODULE_VERSION("2018-03-14-V01");
MODULE_LICENSE("GPL");

