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

static int hall_irq=0;
static unsigned int gpiohall,halldebounce;

static struct switch_dev SLhall_data;
struct wake_lock SLhall_EINT_lock;
static struct work_struct SLhall_eint_work;
static struct workqueue_struct * SLhall_eint_workqueue = NULL;
extern int mt_gpio_set_debounce(unsigned gpio, unsigned debounce);

//test
//cat /sys/devices/virtual/misc/mtgpio/pin    
//cat /sys/class/switch/hall/state 
//NQ16 GPIO145

#define  HALL_CLOSE      KEY_LEFTSHIFT
#define  HALL_OPEN       KEY_RIGHTSHIFT

#ifdef CONFIG_BIRD_HALL_SENSOR_SUPPORT_MX2103
#define  GPIO121_MHALL_EINT_PIN    88
#else
#define  GPIO121_MHALL_EINT_PIN    121
#endif

#define NO_DEVICE		0
#define OPEN      (1)
#define CLOSE     (0)
static int cur_mhall_eint_state = OPEN;
static int cur_gpio_state =0 ;


static struct input_dev *kpd_hall_dev=NULL;

 
static int mhall_debounce_flag=0; 

int get_hall_is_debounce(void)
{
  return mhall_debounce_flag;
}
int get_hall_is_open(void)
{
   return cur_mhall_eint_state;
}

unsigned char key_slid_mode=0;  //global val for TP  1----TP para use TP_slid

static int hall_kpd_input_init(void)
{
	kpd_hall_dev = input_allocate_device();
	if (kpd_hall_dev==NULL) 
	{
		printk("[hall]kpd_hall_dev : fail!\n");
		return -1;
	}
	__set_bit(EV_KEY, kpd_hall_dev->evbit);
	__set_bit(HALL_OPEN,  kpd_hall_dev->keybit);
	__set_bit(HALL_CLOSE, kpd_hall_dev->keybit);

	__set_bit(EV_SW, kpd_hall_dev->evbit);
	__set_bit(SW_LID, kpd_hall_dev->swbit);

	kpd_hall_dev->id.bustype = BUS_HOST;
	kpd_hall_dev->name = "hall";
	if(input_register_device(kpd_hall_dev))
	{
		printk("[hall]kpd_hall_dev register : fail!\n");
	}
	else
	{
		printk("[hall]kpd_hall_dev register : success!!\n");
	} 
	return 1;
}


 void kpd_send_key_hall_open(void)
{
	if (kpd_hall_dev==NULL) 
	{
		printk(" kpd_send_key_hall_open Faill kpd_hall_dev=NULL !!\n");
		return;
	}
		
	printk(" kpd_send_key_hall_open!!\n");
	input_report_key(kpd_hall_dev, HALL_OPEN, 1);
	input_sync(kpd_hall_dev);
	input_report_key(kpd_hall_dev, HALL_OPEN, 0);
	input_sync(kpd_hall_dev);

}

 void kpd_send_key_hall_close(void)
{

	if (kpd_hall_dev==NULL) 
	{
		printk(" kpd_send_key_hall_close Faill kpd_hall_dev=NULL !!\n");
		return;
	}

	printk(" kpd_send_key_hall_close!!\n");
	input_report_key(kpd_hall_dev, HALL_CLOSE, 1);
	input_sync(kpd_hall_dev);
	input_report_key(kpd_hall_dev, HALL_CLOSE, 0);
	input_sync(kpd_hall_dev);

 }


 void kpd_send_old_slid_msg (bool slid)
{
	if (kpd_hall_dev==NULL) 
	{
	printk(" kpd_send_key_hall_open Faill kpd_hall_dev=NULL !!\n");
	return;
	}
	printk(" kpd_send_old_slid_msg slid=%d!!\n",slid);

//	input_report_switch(kpd_hall_dev, SW_LID, slid);
//	input_sync(kpd_hall_dev);
//打开如上注释，磁铁远离HALL器件，wake up LCD

}

 

void SLhall_eint_work_callback(struct work_struct *work)
{


	wake_lock_timeout(&SLhall_EINT_lock, 2*HZ);
	if( cur_mhall_eint_state ==OPEN)  
	{
		printk("****Open******** \n");
		switch_set_state((struct switch_dev *)&SLhall_data, 1);

		kpd_send_key_hall_open();
		kpd_send_old_slid_msg(0);
	}		
	else
	{
		printk("****Close******** \n");

		switch_set_state((struct switch_dev *)&SLhall_data, 0);

		kpd_send_key_hall_close();
		kpd_send_old_slid_msg(1);
	}
	msleep(300);//100ms 

//	enable_irq(hall_irq);
        mhall_debounce_flag=0;
}



static irqreturn_t SLhall_eint_func(int irq,void *data)
{
	int ret=0;


	mhall_debounce_flag=1;

	if (cur_gpio_state)
	{
		cur_gpio_state=0;
		irq_set_irq_type(hall_irq,IRQ_TYPE_LEVEL_HIGH);
	}
	else
	{
		cur_gpio_state=1;
		irq_set_irq_type(hall_irq,IRQ_TYPE_LEVEL_LOW);
	}

	cur_mhall_eint_state=cur_gpio_state;

	if(cur_mhall_eint_state == CLOSE) 
	{
		key_slid_mode=1;
	} 
	else 
	{
		key_slid_mode=0;
	}

	ret = queue_work(SLhall_eint_workqueue, &SLhall_eint_work); 
	return IRQ_HANDLED;
}


static inline int SLhall_setup_eint(void)
{
	int ret=0;


	u32 ints[2]={0,0};
	struct device_node *node;


	printk("[SLhall]SLhall_setup_eint\n");
	                                      

	cur_gpio_state=gpio_get_value(GPIO121_MHALL_EINT_PIN);//mt_get_gpio_in(GPIO_MHALL_EINT_PIN) ;  
	
	cur_mhall_eint_state=  cur_gpio_state;
		
	if(cur_mhall_eint_state==CLOSE)
	{
		  key_slid_mode=1;
	}
	else
	{
	  	key_slid_mode=0;
	}
	
	wake_lock_timeout(&SLhall_EINT_lock, 2*HZ);
	switch_set_state((struct switch_dev *)&SLhall_data, cur_mhall_eint_state);

	if( cur_mhall_eint_state ==OPEN)  
	{
		printk("****Open******** \n");
		kpd_send_old_slid_msg(0);
	}		
	else
	{
		printk("****Close******** \n");
		kpd_send_old_slid_msg(1);
	}

	    


    	 node = of_find_compatible_node(NULL,NULL,"mediatek, mhall-eint");
	 if(node) {
       	 of_property_read_u32_array(node,"debounce",ints,ARRAY_SIZE(ints));
		gpiohall = ints[0];
		halldebounce = ints[1];
		mt_gpio_set_debounce(gpiohall,halldebounce);
		printk("****mt_gpio_set_debounce,gpiopin=%d, debounce=%d*** \n",gpiohall,halldebounce);

		hall_irq = irq_of_parse_and_map(node,0);
		ret = request_irq(hall_irq,SLhall_eint_func,IRQF_TRIGGER_NONE,"MHALL-eint",NULL);
		if(ret>0)
		{
       		     printk("[Hall]EINT IRQ LINE NOT AVAILABLE\n");
		}
		else
		{
			printk("[Hall]Hall set EINT finished, hall_irq=%d, halldebounce=%d \n", hall_irq, halldebounce);
		}
	}
	else 
	{
	        printk("[Hall]%s can't find compatible node\n", __func__);
	}

		if (cur_gpio_state)
		{
			irq_set_irq_type(hall_irq,IRQ_TYPE_LEVEL_LOW);
		}
		else{
			irq_set_irq_type(hall_irq,IRQ_TYPE_LEVEL_HIGH);
		}


       //	enable_irq(hall_irq);



	return 0; 
}

static int SLhall_probe(struct platform_device *dev) 
{
	int ret = 0;
	printk("SLhall_probe begin!\n");
	
	hall_kpd_input_init();
	
	SLhall_data.name = "hall";
	SLhall_data.index = 0;
	SLhall_data.state = NO_DEVICE;
	
	ret = switch_dev_register(&SLhall_data);
	if(ret)
	{
		printk("SLhall switch_dev_register returned:%d!\n", ret);
		return 1;
	}
	else
	{
		printk("SLhall switch_dev_register OK! \n");
             
	}
	
	wake_lock_init(&SLhall_EINT_lock, WAKE_LOCK_SUSPEND, "SLhall EINT wakelock");
	
	printk("SLhall_probe : SLhall_INIT\n"); 
	
	SLhall_eint_workqueue = create_singlethread_workqueue("SLhall_eint");
	INIT_WORK(&SLhall_eint_work, SLhall_eint_work_callback);///////////
	SLhall_setup_eint();
	
	printk("SLhall_probe done!\n"); 
	return 0;
}

static int SLhall_remove(struct platform_device *dev) 
{
	printk("SLhall_remove begin!\n");
	
	destroy_workqueue(SLhall_eint_workqueue);
	switch_dev_unregister(&SLhall_data);
	
	printk("[SLhall]SLhall_remove Done!\n"); 
	return 0;
}


struct platform_device slhall_device = {
	.name	  ="SLhall",
	.id		  = -1,
};

static struct platform_driver SLhall_driver = {
	.probe = SLhall_probe, 
	.remove = SLhall_remove,
	.driver = {
	.name = "SLhall", 
	},
};

static int SLhall_mod_init(void)
{
	int ret = 0;
	printk("SLhall_mod_init begin!\n");

	ret = platform_device_register(&slhall_device);
	printk("register slhall device\n");
	if (ret != 0)
	{
		printk("platform_device_slhall_register error:(%d)\n", ret);
		return ret;
	}
	else
	{
		printk("platform_device_slhall_register done!\n");
	}

	
	ret = platform_driver_register(&SLhall_driver);
	
	if (ret) {
		printk("SLhall platform_driver_register error:(%d)\n", ret);
		return ret;
	}
	else
	{
		printk("SLhall platform_driver_register done!\n");
	}

	printk("SLhall_mod_init done!\n");
	return 0;
}

static void SLhall_mod_exit(void)
{
	printk("SLhall_mod_exit\n");
	
	platform_driver_unregister(&SLhall_driver);
	
	printk("SLhall_mod_exit Done!\n");
}

module_init(SLhall_mod_init);
module_exit(SLhall_mod_exit);

MODULE_DESCRIPTION("XXXX");
MODULE_AUTHOR("XXXX");
MODULE_LICENSE("GPL");

