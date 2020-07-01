#include <linux/init.h>
#include <linux/module.h>
#include <linux/delay.h>
#include <linux/i2c.h>
#include <linux/input.h>
#include <linux/vmalloc.h>
#include <linux/gpio.h>
#include <linux/sched.h>
#include <linux/wakelock.h>
#include <linux/kthread.h>
#include <linux/bitops.h>
#include <linux/kernel.h>
#include <linux/delay.h>
#include <linux/switch.h>
#include <linux/workqueue.h>
#include <linux/byteorder/generic.h>
#ifdef CONFIG_HAS_EARLYSUSPEND
#include <linux/earlysuspend.h>
#endif 
#include <linux/interrupt.h>
#include <linux/time.h>
//#include <linux/rtpm_prio.h>
#include <linux/proc_fs.h>
#include <linux/uaccess.h>
#include <linux/jiffies.h>
#include <linux/platform_device.h>
#include <linux/kthread.h>
#include <linux/cdev.h>
#include <linux/of.h>
#include <linux/of_address.h>
#include <linux/of_irq.h>
/*
#include <mach/mt_boot.h>
#include <mach/mt_gpio.h>
#include <cust_eint.h>
#include <cust_gpio_usage.h>
*/
#include <linux/gpio.h>
#include <linux/device.h>

#include <linux/miscdevice.h>
#include <linux/irq.h>
#include <linux/moduleparam.h>
#include <linux/fs.h>	
#include <linux/errno.h>
#include <linux/types.h>	
#include <linux/fcntl.h>	
#include <linux/aio.h>
#include <linux/unistd.h>

static int toggle_switch_irq=0;
static unsigned int gpiotoggle_switch,toggle_switchdebounce;

static struct switch_dev BDtoggle_switch_data;
struct wake_lock BDtoggle_switch_EINT_lock;
static struct work_struct BDtoggle_switch_eint_work;
static struct workqueue_struct * BDtoggle_switch_eint_workqueue = NULL;
extern int mt_gpio_set_debounce(unsigned gpio, unsigned debounce);

//test
//cat /sys/devices/virtual/misc/mtgpio/pin    
//cat /sys/class/switch/toggle_switch/state 
//NQ16 GPIO145

//#define TOGGLE_SWITCH_POWER_KEY_TEST
#ifdef TOGGLE_SWITCH_POWER_KEY_TEST
#define  TOGGLE_SWITCH_CLOSE      KEY_POWER
#define  TOGGLE_SWITCH_OPEN       KEY_POWER
#else
#define  TOGGLE_SWITCH_CLOSE      KEY_F6
#define  TOGGLE_SWITCH_OPEN       KEY_F7
#endif


#define  GPIO86_TOGGLE_SWITCH_EINT_PIN    86

#define NO_DEVICE		0
#define OPEN      (1)
#define CLOSE     (0)
static int cur_toggle_switch_eint_state = OPEN;
static int cur_gpio_state =0 ;


static struct input_dev *kpd_toggle_switch_dev=NULL;

 
static int toggle_switch_debounce_flag=0; 

int get_toggle_switch_is_debounce(void)
{
  return toggle_switch_debounce_flag;
}
int get_toggle_switch_is_open(void)
{
   return cur_toggle_switch_eint_state;
}

unsigned char key_bdid_mode=0;  //global val for TP  1----TP para use TP_bdid

static int toggle_switch_kpd_input_init(void)
{
	kpd_toggle_switch_dev = input_allocate_device();
	if (kpd_toggle_switch_dev==NULL) 
	{
		printk("[toggle_switch]kpd_toggle_switch_dev : fail!\n");
		return -1;
	}
	__set_bit(EV_KEY, kpd_toggle_switch_dev->evbit);
	__set_bit(TOGGLE_SWITCH_OPEN,  kpd_toggle_switch_dev->keybit);
	__set_bit(TOGGLE_SWITCH_CLOSE, kpd_toggle_switch_dev->keybit);

	__set_bit(EV_SW, kpd_toggle_switch_dev->evbit);
	__set_bit(SW_LID, kpd_toggle_switch_dev->swbit);

	kpd_toggle_switch_dev->id.bustype = BUS_HOST;
	kpd_toggle_switch_dev->name = "toggle_switch";
	if(input_register_device(kpd_toggle_switch_dev))
	{
		printk("[toggle_switch]kpd_toggle_switch_dev register : fail!\n");
	}
	else
	{
		printk("[toggle_switch]kpd_toggle_switch_dev register : success!!\n");
	} 
	return 1;
}


 void kpd_send_key_toggle_switch_open(void)
{
	if (kpd_toggle_switch_dev==NULL) 
	{
		printk(" kpd_send_key_toggle_switch_open Faill kpd_toggle_switch_dev=NULL !!\n");
		return;
	}
		
	printk(" kpd_send_key_toggle_switch_open!!\n");
	input_report_key(kpd_toggle_switch_dev, TOGGLE_SWITCH_OPEN, 1);
	input_sync(kpd_toggle_switch_dev);
	input_report_key(kpd_toggle_switch_dev, TOGGLE_SWITCH_OPEN, 0);
	input_sync(kpd_toggle_switch_dev);

}

 void kpd_send_key_toggle_switch_close(void)
{

	if (kpd_toggle_switch_dev==NULL) 
	{
		printk(" kpd_send_key_toggle_switch_close Faill kpd_toggle_switch_dev=NULL !!\n");
		return;
	}

	printk(" kpd_send_key_toggle_switch_close!!\n");
	input_report_key(kpd_toggle_switch_dev, TOGGLE_SWITCH_CLOSE, 1);
	input_sync(kpd_toggle_switch_dev);
	input_report_key(kpd_toggle_switch_dev, TOGGLE_SWITCH_CLOSE, 0);
	input_sync(kpd_toggle_switch_dev);

 }


 void kpd_send_old_bdid_msg (bool bdid)
{
	if (kpd_toggle_switch_dev==NULL) 
	{
	printk(" kpd_send_key_toggle_switch_open Faill kpd_toggle_switch_dev=NULL !!\n");
	return;
	}
	printk(" kpd_send_old_bdid_msg bdid=%d!!\n",bdid);

//	input_report_switch(kpd_toggle_switch_dev, SW_LID, bdid);
//	input_sync(kpd_toggle_switch_dev);
//打开如上注释，磁铁远离TOGGLE_SWITCH器件，wake up LCD

}

 

void BDtoggle_switch_eint_work_callback(struct work_struct *work)
{


	wake_lock_timeout(&BDtoggle_switch_EINT_lock, 2*HZ);
	if( cur_toggle_switch_eint_state ==OPEN)  
	{
		printk("****Open******** \n");
		switch_set_state((struct switch_dev *)&BDtoggle_switch_data, 1);

		kpd_send_key_toggle_switch_open();
		kpd_send_old_bdid_msg(0);
	}		
	else
	{
		printk("****Close******** \n");

		switch_set_state((struct switch_dev *)&BDtoggle_switch_data, 0);

		kpd_send_key_toggle_switch_close();
		kpd_send_old_bdid_msg(1);
	}
	msleep(300);//100ms 

//	enable_irq(toggle_switch_irq);
        toggle_switch_debounce_flag=0;
}



static irqreturn_t BDtoggle_switch_eint_func(int irq,void *data)
{
	int ret=0;


	toggle_switch_debounce_flag=1;

	if (cur_gpio_state)
	{
		cur_gpio_state=0;
		irq_set_irq_type(toggle_switch_irq,IRQ_TYPE_LEVEL_HIGH);
	}
	else
	{
		cur_gpio_state=1;
		irq_set_irq_type(toggle_switch_irq,IRQ_TYPE_LEVEL_LOW);
	}

	cur_toggle_switch_eint_state=cur_gpio_state;

	if(cur_toggle_switch_eint_state == CLOSE) 
	{
		key_bdid_mode=1;
	} 
	else 
	{
		key_bdid_mode=0;
	}

	ret = queue_work(BDtoggle_switch_eint_workqueue, &BDtoggle_switch_eint_work); 
	return IRQ_HANDLED;
}


static inline int BDtoggle_switch_setup_eint(void)
{
	int ret=0;


	u32 ints[2]={0,0};
	struct device_node *node;


	printk("[BDtoggle_switch]BDtoggle_switch_setup_eint\n");
	                                      

	cur_gpio_state=gpio_get_value(GPIO86_TOGGLE_SWITCH_EINT_PIN);//mt_get_gpio_in(GPIO_TOGGLE_SWITCH_EINT_PIN) ;  
	
	cur_toggle_switch_eint_state=  cur_gpio_state;
		
	if(cur_toggle_switch_eint_state==CLOSE)
	{
		  key_bdid_mode=1;
	}
	else
	{
	  	key_bdid_mode=0;
	}
	
	wake_lock_timeout(&BDtoggle_switch_EINT_lock, 2*HZ);
	switch_set_state((struct switch_dev *)&BDtoggle_switch_data, cur_toggle_switch_eint_state);

	if( cur_toggle_switch_eint_state ==OPEN)  
	{
		printk("****Open******** \n");
		kpd_send_old_bdid_msg(0);
	}		
	else
	{
		printk("****Close******** \n");
		kpd_send_old_bdid_msg(1);
	}

	    


    	 node = of_find_compatible_node(NULL,NULL,"mediatek, PTT_EINT-eint");
	 if(node) {
       	 of_property_read_u32_array(node,"debounce",ints,ARRAY_SIZE(ints));
		gpiotoggle_switch = ints[0];
		toggle_switchdebounce = ints[1];
		mt_gpio_set_debounce(gpiotoggle_switch,toggle_switchdebounce);
		printk("****mt_gpio_set_debounce,gpiopin=%d, debounce=%d*** \n",gpiotoggle_switch,toggle_switchdebounce);

		toggle_switch_irq = irq_of_parse_and_map(node,0);
		ret = request_irq(toggle_switch_irq,BDtoggle_switch_eint_func,IRQF_TRIGGER_NONE,"PTT_EINT-eint",NULL);
		if(ret>0)
		{
       		     printk("[Toggle_switch]EINT IRQ LINE NOT AVAILABLE\n");
		}
		else
		{
			printk("[Toggle_switch]Toggle_switch set EINT finished, toggle_switch_irq=%d, toggle_switchdebounce=%d \n", toggle_switch_irq, toggle_switchdebounce);
		}
	}
	else 
	{
	        printk("[Toggle_switch]%s can't find compatible node\n", __func__);
	}

		if (cur_gpio_state)
		{
			irq_set_irq_type(toggle_switch_irq,IRQ_TYPE_LEVEL_LOW);
		}
		else{
			irq_set_irq_type(toggle_switch_irq,IRQ_TYPE_LEVEL_HIGH);
		}


       //	enable_irq(toggle_switch_irq);



	return 0; 
}

static int BDtoggle_switch_probe(struct platform_device *dev) 
{
	int ret = 0;
	printk("BDtoggle_switch_probe begin!\n");
	
	toggle_switch_kpd_input_init();
	
	BDtoggle_switch_data.name = "toggle_switch";
	BDtoggle_switch_data.index = 0;
	BDtoggle_switch_data.state = NO_DEVICE;
	
	ret = switch_dev_register(&BDtoggle_switch_data);
	if(ret)
	{
		printk("BDtoggle_switch switch_dev_register returned:%d!\n", ret);
		return 1;
	}
	else
	{
		printk("BDtoggle_switch switch_dev_register OK! \n");
             
	}
	
	wake_lock_init(&BDtoggle_switch_EINT_lock, WAKE_LOCK_SUSPEND, "BDtoggle_switch EINT wakelock");
	
	printk("BDtoggle_switch_probe : BDtoggle_switch_INIT\n"); 
	
	BDtoggle_switch_eint_workqueue = create_singlethread_workqueue("BDtoggle_switch_eint");
	INIT_WORK(&BDtoggle_switch_eint_work, BDtoggle_switch_eint_work_callback);///////////
	BDtoggle_switch_setup_eint();
	
	printk("BDtoggle_switch_probe done!\n"); 
	return 0;
}

static int BDtoggle_switch_remove(struct platform_device *dev) 
{
	printk("BDtoggle_switch_remove begin!\n");
	
	destroy_workqueue(BDtoggle_switch_eint_workqueue);
	switch_dev_unregister(&BDtoggle_switch_data);
	
	printk("[BDtoggle_switch]BDtoggle_switch_remove Done!\n"); 
	return 0;
}


struct platform_device bdtoggle_switch_device = {
	.name	  ="BDtoggle_switch",
	.id		  = -1,
};

static struct platform_driver BDtoggle_switch_driver = {
	.probe = BDtoggle_switch_probe, 
	.remove = BDtoggle_switch_remove,
	.driver = {
	.name = "BDtoggle_switch", 
	},
};

static int BDtoggle_switch_mod_init(void)
{
	int ret = 0;
	printk("BDtoggle_switch_mod_init begin!\n");

	ret = platform_device_register(&bdtoggle_switch_device);
	printk("register bdtoggle_switch device\n");
	if (ret != 0)
	{
		printk("platform_device_bdtoggle_switch_register error:(%d)\n", ret);
		return ret;
	}
	else
	{
		printk("platform_device_bdtoggle_switch_register done!\n");
	}

	
	ret = platform_driver_register(&BDtoggle_switch_driver);
	
	if (ret) {
		printk("BDtoggle_switch platform_driver_register error:(%d)\n", ret);
		return ret;
	}
	else
	{
		printk("BDtoggle_switch platform_driver_register done!\n");
	}

	printk("BDtoggle_switch_mod_init done!\n");
	return 0;
}

static void BDtoggle_switch_mod_exit(void)
{
	printk("BDtoggle_switch_mod_exit\n");
	
	platform_driver_unregister(&BDtoggle_switch_driver);
	
	printk("BDtoggle_switch_mod_exit Done!\n");
}

module_init(BDtoggle_switch_mod_init);
module_exit(BDtoggle_switch_mod_exit);

MODULE_DESCRIPTION("XXXX");
MODULE_AUTHOR("XXXX");
MODULE_LICENSE("GPL");

