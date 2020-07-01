#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/types.h>
#include <linux/wait.h>
#include <linux/slab.h>
#include <linux/fs.h>
#include <linux/sched.h>
#include <linux/poll.h>
#include <linux/device.h>
#include <linux/interrupt.h>
#include <linux/delay.h>
#include <linux/platform_device.h>
#include <linux/cdev.h>
#include <linux/errno.h>
#include <linux/time.h>
#include "kd_flashlight.h"
#include <asm/io.h>
#include <asm/uaccess.h>
#include "kd_camera_typedef.h"
#include <linux/hrtimer.h>
#include <linux/ktime.h>
#include <linux/version.h>
#include <linux/mutex.h>
#include <linux/i2c.h>
#include <linux/leds.h>
#include <linux/miscdevice.h>



/******************************************************************************
 * Debug configuration
******************************************************************************/
/* availible parameter */
/* ANDROID_LOG_ASSERT */
/* ANDROID_LOG_ERROR */
/* ANDROID_LOG_WARNING */
/* ANDROID_LOG_INFO */
/* ANDROID_LOG_DEBUG */
/* ANDROID_LOG_VERBOSE */

#define TAG_NAME "[leds_strobe.c]"
#define PK_DBG_NONE(fmt, arg...)    do {} while (0)
#define PK_DBG_FUNC(fmt, arg...)    pr_debug(TAG_NAME "%s: " fmt, __func__ , ##arg)
#define PK_WARN(fmt, arg...)        pr_warning(TAG_NAME "%s: " fmt, __func__ ,##arg)
#define PK_NOTICE(fmt, arg...)      pr_notice(TAG_NAME "%s: " fmt, __func__ ,##arg)
#define PK_INFO(fmt, arg...)        pr_info(TAG_NAME "%s: " fmt, __func__ ,##arg)
#define PK_TRC_FUNC(f)              pr_debug(TAG_NAME "<%s>\n", __func__)
#define PK_TRC_VERBOSE(fmt, arg...) pr_debug(TAG_NAME fmt, ##arg)
#define PK_ERROR(fmt, arg...)       pr_err(TAG_NAME "%s: " fmt, __func__ ,##arg)


#define DEBUG_LEDS_STROBE
#ifdef  DEBUG_LEDS_STROBE
	#define PK_DBG PK_DBG_FUNC
	#define PK_VER PK_TRC_VERBOSE
	#define PK_ERR PK_ERROR
#else
	#define PK_DBG(a,...)
	#define PK_VER(a,...)
	#define PK_ERR(a,...)
#endif

/******************************************************************************
 * local variables
******************************************************************************/

static DEFINE_SPINLOCK(g_strobeSMPLock); /* cotta-- SMP proection */


static u32 strobe_Res = 0;
static u32 strobe_Timeus = 0;
static BOOL g_strobe_On = 0;

static int g_timeOutTimeMs=0;

extern struct pinctrl *flashlight_pinctrl;
extern struct pinctrl_state *flashlight_high;
extern struct pinctrl_state *flashlight_low;
#ifdef CONFIG_DUAL_REAL_FLASHLIGHT
static int g_duty=-1;
static int g_step=-1;
extern struct pinctrl_state *flashstrobe_high;
extern struct pinctrl_state *flashstrobe_low;
#endif
static void work_timeOutFunc(struct work_struct *data);

static DEFINE_MUTEX(g_strobeSem);


#define STROBE_DEVICE_ID 0xC6

/*
#if defined(GPIO_CAMERA_FLASH_EN_PIN)
	#define GPIO_CAMERA_FLASH_EN GPIO_CAMERA_FLASH_EN_PIN
#else
	#define GPIO_CAMERA_FLASH_EN GPIO43
#endif
*/

extern int iWriteRegI2C(u8 *a_pSendData , u16 a_sizeSendData, u16 i2cId);
extern int iReadRegI2C(u8 *a_pSendData , u16 a_sizeSendData, u8 * a_pRecvData, u16 a_sizeRecvData, u16 i2cId);
static struct work_struct workTimeOut;


/*****************************************************************************
Functions
*****************************************************************************/
enum hrtimer_restart ledTimeOutCallback(struct hrtimer *timer)
{
	PK_DBG("ledTimeOut_callback\n");
	schedule_work(&workTimeOut);

    return HRTIMER_NORESTART;
}

static struct hrtimer g_timeOutTimer;
void timerInit(void)
{
  	INIT_WORK(&workTimeOut, work_timeOutFunc);
	g_timeOutTimeMs=1000; //1s
	hrtimer_init( &g_timeOutTimer, CLOCK_MONOTONIC, HRTIMER_MODE_REL );
	g_timeOutTimer.function=ledTimeOutCallback;

}


int FL_Enable(void)
{
/*
    mt_set_gpio_mode(GPIO_CAMERA_FLASH_EN,GPIO_MODE_00);
    mt_set_gpio_dir(GPIO_CAMERA_FLASH_EN,GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CAMERA_FLASH_EN,1);
*/
	pinctrl_select_state(flashlight_pinctrl, flashlight_high);
	return 0;
}


int FL_Disable(void)
{
/*
    mt_set_gpio_mode(GPIO_CAMERA_FLASH_EN,GPIO_MODE_00);
    mt_set_gpio_dir(GPIO_CAMERA_FLASH_EN,GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CAMERA_FLASH_EN,GPIO_OUT_ZERO);
*/
	pinctrl_select_state(flashlight_pinctrl, flashlight_low);
	return 0;
}



#ifdef CONFIG_DUAL_REAL_FLASHLIGHT

	#define LEDS_TORCH_MODE 		1
	#define LEDS_FLASH_MODE 		0
	#define LEDS_CUSTOM_MODE_THRES 	40
/*
	#ifndef GPIO_CAMERA_FLASH_EN_PIN 
	#define GPIO_CAMERA_FLASH_EN_PIN GPIO43
	#endif

	#ifndef GPIO_CAMERA_FLASH_EN_PIN_M_GPIO 
	#define GPIO_CAMERA_FLASH_EN_PIN_M_GPIO GPIO_MODE_00
	#endif

	#ifndef GPIO_CAMERA_FLASH_MODE_PIN 
	#define GPIO_CAMERA_FLASH_MODE_PIN GPIO42
	#endif

	#ifndef GPIO_CAMERA_FLASH_MODE_PIN_M_GPIO 
	#define GPIO_CAMERA_FLASH_MODE_PIN_M_GPIO GPIO_MODE_00
	#endif
*/
	int FL_enable(void)
	{
		PK_DBG("FL_enable");	
		//mt_set_gpio_out(GPIO_CAMERA_FLASH_EN_PIN, 1);
		pinctrl_select_state(flashlight_pinctrl, flashlight_high);

	    	return 0;
	}

	int FL_enable_torch(void)
	{
		PK_DBG("FL_torch_enable");	
		//mt_set_gpio_out(GPIO_CAMERA_FLASH_EN_PIN, 1);
		//mt_set_gpio_out(GPIO_CAMERA_FLASH_MODE_PIN, 1);	
		//pinctrl_select_state(flashlight_pinctrl, flashlight_high);
		pinctrl_select_state(flashlight_pinctrl, flashstrobe_high);

	    	return 0;
	}

	int FL_disable(void)
	{
		PK_DBG("FL_disable");
		//mt_set_gpio_out(GPIO_CAMERA_FLASH_EN_PIN, 0);
		//mt_set_gpio_out(GPIO_CAMERA_FLASH_MODE_PIN, 0);	
		pinctrl_select_state(flashlight_pinctrl, flashlight_low);
		pinctrl_select_state(flashlight_pinctrl, flashstrobe_low);

	    	return 0;
	}

	int FL_disable_torch(void)
	{
		PK_DBG("FL_torch_disable");
		//mt_set_gpio_out(GPIO_CAMERA_FLASH_EN_PIN, 0);
		//mt_set_gpio_out(GPIO_CAMERA_FLASH_MODE_PIN, 0);	
		//pinctrl_select_state(flashlight_pinctrl, flashlight_low);
		pinctrl_select_state(flashlight_pinctrl, flashstrobe_low);

	    	return 0;
	}

	int FL_dim_duty(kal_uint32 duty)
	{
		PK_DBG("FL_dim_duty %d, thres %d", duty, LEDS_CUSTOM_MODE_THRES);
	
		if(duty < LEDS_CUSTOM_MODE_THRES) {	
			//mt_set_gpio_out(GPIO_CAMERA_FLASH_MODE_PIN, LEDS_TORCH_MODE);
			pinctrl_select_state(flashlight_pinctrl, flashstrobe_high);
		} else {
			//mt_set_gpio_out(GPIO_CAMERA_FLASH_MODE_PIN, LEDS_FLASH_MODE);
			pinctrl_select_state(flashlight_pinctrl, flashstrobe_low);	
		}

		if((g_timeOutTimeMs == 0) && (duty > LEDS_CUSTOM_MODE_THRES))
		{
			PK_ERR("FL_dim_duty %d > thres %d, FLASH mode but timeout %d", duty, LEDS_CUSTOM_MODE_THRES, g_timeOutTimeMs);	
			//mt_set_gpio_out(GPIO_CAMERA_FLASH_MODE_PIN, LEDS_TORCH_MODE);
			pinctrl_select_state(flashlight_pinctrl, flashstrobe_high);
		}	

	    	return 0;
	}

	int FL_step(kal_uint32 step)
	{
		//int sTab[8]={0,2,4,6,9,11,13,15};
		PK_DBG("FL_step");

	    	return 0;
	}

	int FL_init(void)
	{

		PK_DBG("FL_init");

		//mt_set_gpio_mode(GPIO_CAMERA_FLASH_EN_PIN, GPIO_CAMERA_FLASH_EN_PIN_M_GPIO);
		//mt_set_gpio_mode(GPIO_CAMERA_FLASH_MODE_PIN, GPIO_CAMERA_FLASH_MODE_PIN_M_GPIO);

		FL_disable();
		INIT_WORK(&workTimeOut, work_timeOutFunc);
	    	return 0;
	}

	int FL_uninit(void)
	{
		PK_DBG("FL_uninit");

		FL_disable();
	    	return 0;
	}

	static void work_timeOutFunc(struct work_struct *data)
	{
		FL_disable();
	    	PK_DBG("ledTimeOut_callback\n");
	    //printk(KERN_ALERT "work handler function./n");
	}
	
	static int constant_flashlight_ioctl(unsigned int cmd, unsigned long arg)
	{
		int i4RetValue = 0;
		int iFlashType = (int)FLASHLIGHT_NONE;
		int ior;
		int iow;
		int iowr;
		ior = _IOR(FLASHLIGHT_MAGIC,0, int);
		iow = _IOW(FLASHLIGHT_MAGIC,0, int);
		iowr = _IOWR(FLASHLIGHT_MAGIC,0, int);
		PK_DBG("constant_flashlight_ioctl() line=%d cmd=%d, ior=%d, iow=%d iowr=%d arg=%ld\n",__LINE__, cmd, ior, iow, iowr, arg);
		PK_DBG("constant_flashlight_ioctl() line=%d cmd-ior=%d, cmd-iow=%d cmd-iowr=%d arg=%ld\n",__LINE__, cmd-ior, cmd-iow, cmd-iowr, arg);
    		switch(cmd)
    		{

		case FLASH_IOC_SET_TIME_OUT_TIME_MS:
			PK_DBG("FLASH_IOC_SET_TIME_OUT_TIME_MS: %d\n",(int)arg);
			g_timeOutTimeMs=arg;
			break;


    		case FLASH_IOC_SET_DUTY :
	    		PK_DBG("FLASHLIGHT_DUTY: %d\n",(int)arg);
	    		g_duty=arg;
	    		FL_dim_duty(arg);
	    		break;


    		case FLASH_IOC_SET_STEP:
	    		PK_DBG("FLASH_IOC_SET_STEP: %d\n",(int)arg);
	    		g_step=arg;
	    		FL_step(arg);
	    		break;

    		case FLASH_IOC_SET_ONOFF :
	    		PK_DBG("FLASHLIGHT_ONOFF: %d\n",(int)arg);
	    		if(arg==1)
	    		{

	    		    int s;
	    		    int ms;
	    		    if(g_timeOutTimeMs>1000)
		    	    {
		    		s = g_timeOutTimeMs/1000;
		    		ms = g_timeOutTimeMs - s*1000;
		    	    }
		    	    else
		    	    {
		    		s = 0;
		    		ms = g_timeOutTimeMs;
		    	    }

			    if(g_timeOutTimeMs!=0)
			    {
			    	ktime_t ktime;
				ktime = ktime_set( s, ms*1000000 );
				hrtimer_start( &g_timeOutTimer, ktime, HRTIMER_MODE_REL );
	    			FL_enable();
	    			FL_enable_torch();
	    			g_strobe_On=1;
			    }
			    else if(g_timeOutTimeMs==0)
			    {
	    			FL_enable_torch();
	    			g_strobe_On=1;
			    } 
	   		}
	    		else
	    		{
    			    FL_disable();
			    hrtimer_cancel( &g_timeOutTimer );
			    g_strobe_On=0;
	    		}
	    		break;
        	case FLASHLIGHTIOC_G_FLASHTYPE:
			iFlashType = FLASHLIGHT_LED_CONSTANT;
			if(copy_to_user((void __user *) arg , (void*)&iFlashType , _IOC_SIZE(cmd)))
			{
				PK_DBG("[strobe_ioctl] ioctl copy to user failed\n");
				return -EFAULT;
			}
			break;			
		default :
	    		PK_DBG(" No such command \n");
	    		i4RetValue = -EPERM;
	    		break;
    }
    return i4RetValue;
}
#else  //!CONFIG_DUAL_REAL_FLASHLIGHT

	int readReg(int reg)
	{

	    char buf[2];
	    char bufR[2];
	    buf[0]=reg;
	    iReadRegI2C(buf , 1, bufR,1, STROBE_DEVICE_ID);
	    //PK_DBG("qq reg=%d val=%d qq\n", buf[0],bufR[0]);
	    return (int)bufR[0];
	}

	int FL_dim_duty(kal_uint32 duty)
	{
		//PK_DBG(" FL_dim_duty line=%d\n",__LINE__);
		//g_duty = duty;
	    	return 0;
	}


	int FL_Init(void)
	{

		FL_Disable();//FL_Enable();
		INIT_WORK(&workTimeOut, work_timeOutFunc);
	    	return 0;
	}


	int FL_Uninit(void)
	{
		FL_Disable();
	    	return 0;
	}
	
	static void work_timeOutFunc(struct work_struct *data)
	{
	    FL_Disable();
	    PK_DBG("ledTimeOut_callback\n");
	    //printk(KERN_ALERT "work handler function./n");
	}
	
	static int constant_flashlight_ioctl(unsigned int cmd, unsigned long arg)
	{
	int i4RetValue = 0;
	int ior_shift;
	int iow_shift;
	int iowr_shift;
	ior_shift = cmd - (_IOR(FLASHLIGHT_MAGIC,0, int));
	iow_shift = cmd - (_IOW(FLASHLIGHT_MAGIC,0, int));
	iowr_shift = cmd - (_IOWR(FLASHLIGHT_MAGIC,0, int));
	PK_DBG("LM3642 constant_flashlight_ioctl() line=%d ior_shift=%d, iow_shift=%d iowr_shift=%d arg=%d\n",
						__LINE__, ior_shift, iow_shift, iowr_shift,(int)arg);
    	switch(cmd)
    	{

		case FLASH_IOC_SET_TIME_OUT_TIME_MS:
			PK_DBG("FLASH_IOC_SET_TIME_OUT_TIME_MS: %d\n",(int)arg);
			g_timeOutTimeMs=arg;
		break;


    		case FLASH_IOC_SET_DUTY :
	    		PK_DBG("FLASHLIGHT_DUTY: %d\n",(int)arg);
	    		FL_dim_duty(arg);
	    		break;


    		case FLASH_IOC_SET_STEP:
	    		PK_DBG("FLASH_IOC_SET_STEP: %d\n",(int)arg);

	    		break;

    		case FLASH_IOC_SET_ONOFF :
	    		PK_DBG("FLASHLIGHT_ONOFF: %d\n",(int)arg);
	    		if(arg==1)
	    		{

	    		    int s;
	    		    int ms;
	    		    if(g_timeOutTimeMs>1000)
		    	    {
		    		s = g_timeOutTimeMs/1000;
		    		ms = g_timeOutTimeMs - s*1000;
		    	    }
		    	    else
		    	    {
		    		s = 0;
		    		ms = g_timeOutTimeMs;
		    	    }

			    if(g_timeOutTimeMs!=0)
			    {
			    	ktime_t ktime;
				ktime = ktime_set( s, ms*1000000 );
				hrtimer_start( &g_timeOutTimer, ktime, HRTIMER_MODE_REL );
			    }
	    		    FL_Enable();
	    		}
	    		else
	    		{
	    		    FL_Disable();
			    hrtimer_cancel( &g_timeOutTimer );
	    		}
	    		break;
		default :
	    		PK_DBG(" No such command \n");
	    		i4RetValue = -EPERM;
	    		break;
    }
    return i4RetValue;
}

#endif //end CONFIG_DUAL_REAL_FLASHLIGHT



//#add by liangjiaqiang for torch mode begin
//test 
//echo C > dev/mainled 
//echo D > dev/mainled 

#define DEVICE_NAME				"mainled"
static u8 bird_mainled_flag=0x00; 
static ssize_t constant_flashlight_write(struct file * file,const char * buf, size_t count,loff_t * f_ops)
{
	unsigned long missing;
	char inbuffer[4]={0};
	int i;
	if(count>4)count=4;
		missing = copy_from_user(inbuffer, buf, count);

	for(i=0;i<count;i++)
	{
	    printk("**bird_subled_write[%d]=%d **/r/n",i,inbuffer[i]);
	} 

	if(inbuffer[0]==0x43)//C
	{
		 bird_mainled_flag=1;
	#ifdef CONFIG_DUAL_REAL_FLASHLIGHT
		 FL_enable_torch();
	#else
		 FL_Enable();
	#endif
	}
	else if(inbuffer[0]==0x44)//D
	{
		bird_mainled_flag=0;
	#ifdef CONFIG_DUAL_REAL_FLASHLIGHT
		FL_disable_torch();
	#else
		FL_Disable();
	#endif
	}
	printk("**bird_mainled_flag=%d **/r/n",bird_mainled_flag);


	return count;
}

static ssize_t constant_flashlight_read(struct file * file,char * buf,size_t count,loff_t * f_ops)
{
		char sdas[4]={0,0,0,0};
		ssize_t            status = 1;
		unsigned long    missing;

		
		sdas[0]=bird_mainled_flag;
		missing = copy_to_user(buf, sdas, status);
		if (missing == status)
		status = -EFAULT;
		else
		status = status - missing;   
		printk("**bird_mainled_read= %d **/r/n",bird_mainled_flag);
		return status;
}

//#add by liangjiaqiang for torch mode end
/*****************************************************************************
User interface
*****************************************************************************/


/*****************************************************************************
User interface
*****************************************************************************/


static int constant_flashlight_open(void *pArg)
{
	int i4RetValue = 0;
	PK_DBG("constant_flashlight_open line=%d\n", __LINE__);

	if (0 == strobe_Res)
	{
	#ifdef CONFIG_DUAL_REAL_FLASHLIGHT
	    FL_init();
	#else
	    FL_Init();
	#endif
	    timerInit();
	}

	PK_DBG("constant_flashlight_open line=%d\n", __LINE__);
	spin_lock_irq(&g_strobeSMPLock);

	if(strobe_Res)
	{
		PK_ERR(" busy!\n");
		i4RetValue = -EBUSY;
	}
	else
	{
		strobe_Res += 1;
	}

	spin_unlock_irq(&g_strobeSMPLock);
	PK_DBG("constant_flashlight_open line=%d\n", __LINE__);

	return i4RetValue;

}


static int constant_flashlight_release(void *pArg)
{
	PK_DBG(" constant_flashlight_release\n");

	if (strobe_Res)
	{
		spin_lock_irq(&g_strobeSMPLock);

		strobe_Res = 0;
		strobe_Timeus = 0;

		/* LED On Status */
		g_strobe_On = FALSE;

		spin_unlock_irq(&g_strobeSMPLock);
		#ifdef CONFIG_DUAL_REAL_FLASHLIGHT
		    FL_uninit();
		#else
		    FL_Uninit();
		#endif
	}

	PK_DBG(" Done\n");

	return 0;

}

static int bird_constant_flashlight_open(struct inode *inode, struct file *file)
{
	printk("maincameraled---open OK---\n");
	return 0;
}

static int bird_constant_flashlight_close(struct inode *inode, struct file *file) {

	printk("maincameraled---close---\n");
	return 0;
}

FLASHLIGHT_FUNCTION_STRUCT	constantFlashlightFunc=
{
	constant_flashlight_open,
	constant_flashlight_release,
	constant_flashlight_ioctl
};

static struct file_operations constant_flashlight_ops = {
	.owner			= THIS_MODULE,
	.open			= bird_constant_flashlight_open,
	.release		= bird_constant_flashlight_close, 
	.write                  = constant_flashlight_write,
	.read			= constant_flashlight_read
	
};

static struct miscdevice constant_flashlight_dev = {
	.minor = MISC_DYNAMIC_MINOR,
	.name = DEVICE_NAME,
	.fops = &constant_flashlight_ops,
};

MUINT32 constantFlashlightInit(PFLASHLIGHT_FUNCTION_STRUCT *pfFunc)
{
	if (pfFunc != NULL)
	{
		*pfFunc = &constantFlashlightFunc;
	}
	return 0;
}

static int __init constant_flashlight_dev_init(void) {
	int ret;
//	ret = misc_register(&ET_misc_dev);
	if((ret = misc_register(&constant_flashlight_dev)))
	{
		printk("maincameraled: misc_register register failed\n");
	}

	printk("maincameraled initialized\n");

	return ret;
}

static void __exit constant_flashlight_dev_exit(void) {
	misc_deregister(&constant_flashlight_dev);	
}

module_init(constant_flashlight_dev_init);
module_exit(constant_flashlight_dev_exit);

/* LED flash control for high current capture mode*/
ssize_t strobe_VDIrq(void)
{
	return 0;
}

EXPORT_SYMBOL(strobe_VDIrq);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("BIRD Inc.");
MODULE_DESCRIPTION("BIRD mainled Driver");
