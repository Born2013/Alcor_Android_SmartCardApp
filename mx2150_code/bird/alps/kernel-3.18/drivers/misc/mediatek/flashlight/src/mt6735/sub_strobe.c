
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
#include <asm/io.h>
#include <asm/uaccess.h>
#include "kd_camera_typedef.h"
#include <linux/hrtimer.h>
#include <linux/ktime.h>
#include <linux/version.h>
#ifdef CONFIG_COMPAT
#include <linux/fs.h>
#include <linux/compat.h>
#endif
#include "kd_flashlight.h"
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
#define TAG_NAME "[sub_strobe.c]"
#define PK_DBG_NONE(fmt, arg...)    do {} while (0)
#define PK_DBG_FUNC(fmt, arg...)    pr_debug(TAG_NAME "%s: " fmt, __func__ , ##arg)
#define PK_WARN(fmt, arg...)        pr_warn(TAG_NAME "%s: " fmt, __func__ , ##arg)
#define PK_NOTICE(fmt, arg...)      pr_notice(TAG_NAME "%s: " fmt, __func__ , ##arg)
#define PK_INFO(fmt, arg...)        pr_info(TAG_NAME "%s: " fmt, __func__ , ##arg)
#define PK_TRC_FUNC(f)              pr_debug(TAG_NAME "<%s>\n", __func__)
#define PK_TRC_VERBOSE(fmt, arg...) pr_debug(TAG_NAME fmt, ##arg)
#define PK_ERROR(fmt, arg...)       pr_err(TAG_NAME "%s: " fmt, __func__ , ##arg)

#define DEBUG_LEDS_STROBE
#ifdef DEBUG_LEDS_STROBE
#define PK_DBG PK_DBG_FUNC
#define PK_VER PK_TRC_VERBOSE
#define PK_ERR PK_ERROR
#else
#define PK_DBG(a, ...)
#define PK_VER(a, ...)
#define PK_ERR(a, ...)
#endif

#ifdef CONFIG_CUSTOM_KERNEL_SUBFLASHLIGHT

/******************************************************************************
 * local variables
******************************************************************************/

static DEFINE_SPINLOCK(g_strobeSMPLock); /* cotta-- SMP proection */


static u32 strobe_Res = 0;
static u32 strobe_Timeus = 0;
static BOOL g_strobe_On = 0;

//static int g_duty=-1;
static int g_timeOutTimeMs=0;

#if (LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,37))
static DEFINE_MUTEX(g_strobeSem);
#else
static DECLARE_MUTEX(g_strobeSem);
#endif


#define STROBE_DEVICE_ID 0xC6


static struct work_struct workTimeOut;

extern struct pinctrl *flashlight_pinctrl;
extern struct pinctrl_state *subflashlight_high;
extern struct pinctrl_state *subflashlight_low;

/*
struct pinctrl *subflashlight_pinctrl;
struct pinctrl_state *subflashlight_high;
struct pinctrl_state *subflashlight_low;

int subflashlight_gpio_init(struct platform_device *pdev)
{
	int ret = 0;

	subflashlight_pinctrl = devm_pinctrl_get(&pdev->dev);
	if (IS_ERR(subflashlight_pinctrl)) {
		ret = PTR_ERR(subflashlight_pinctrl);
		pr_debug("Cannot find subflashlight pinctrl!");
	}

	subflashlight_high = pinctrl_lookup_state(subflashlight_pinctrl, "subflash_high");
	if (IS_ERR(subflashlight_high)) {
		ret = PTR_ERR(subflashlight_high);
		pr_debug("%s : init err, subflashlight_high\n", __func__);
	}

	subflashlight_low = pinctrl_lookup_state(subflashlight_pinctrl, "suflash_low");
	if (IS_ERR(subflashlight_low)) {
		ret = PTR_ERR(subflashlight_low);
		pr_debug("%s : init err, subflashlight_low\n", __func__);
	}

	return ret;
}
*/


//#define FLASH_GPIO_ENF GPIO12
//#define FLASH_GPIO_ENT GPIO13

//static int g_bLtVersion=0;

/*****************************************************************************
Functions
*****************************************************************************/
//extern int iWriteRegI2C(u8 *a_pSendData , u16 a_sizeSendData, u16 i2cId);
//extern int iReadRegI2C(u8 *a_pSendData , u16 a_sizeSendData, u8 * a_pRecvData, u16 a_sizeRecvData, u16 i2cId);
static void work_timeOutFunc(struct work_struct *data);
//#define GPIO_CAMERA_FLASH_EN GPIO_CAMERA_FLASH_EN_SUB_PIN  //GPIO64(vh2512)
#if 0
static int readReg(int reg)
{
    char buf[2];
    char bufR[2];
    buf[0]=reg;
    iReadRegI2C(buf , 1, bufR,1, STROBE_DEVICE_ID);
    //PK_DBG("qq reg=%d val=%d qq\n", buf[0],bufR[0]);
    return (int)bufR[0];
}
#endif
int sub_strobe_Enable(void)
{
/*
    mt_set_gpio_mode(GPIO_CAMERA_FLASH_EN,GPIO_MODE_00);
    mt_set_gpio_dir(GPIO_CAMERA_FLASH_EN,GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CAMERA_FLASH_EN,1);
*/

    pinctrl_select_state(flashlight_pinctrl, subflashlight_high);

    return 0;

}



int sub_strobe_Disable(void)
{
/*
    mt_set_gpio_mode(GPIO_CAMERA_FLASH_EN,GPIO_MODE_00);
    mt_set_gpio_dir(GPIO_CAMERA_FLASH_EN,GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CAMERA_FLASH_EN,GPIO_OUT_ZERO);
*/

    pinctrl_select_state(flashlight_pinctrl, subflashlight_low);

    return 0;

}
#endif //CONFIG_CUSTOM_KERNEL_SUBFLASHLIGHT

int sub_strobe_dim_duty(kal_uint32 duty)
{
	PK_DBG(" sub_strobe_dim_duty line=%d\n",__LINE__);
	//g_duty = duty;
    return 0;
}


#ifdef CONFIG_CUSTOM_KERNEL_SUBFLASHLIGHT

int sub_strobe_Init(void)
{

	sub_strobe_Disable();
	INIT_WORK(&workTimeOut, work_timeOutFunc);
	return 0;

}


int sub_strobe_Uninit(void)
{
	sub_strobe_Disable();
    return 0;
}

/*****************************************************************************
User interface
*****************************************************************************/

static void work_timeOutFunc(struct work_struct *data)
{
    sub_strobe_Disable();
    PK_DBG("ledTimeOut_callback\n");
    //printk(KERN_ALERT "work handler function./n");
}



static enum hrtimer_restart ledTimeOutCallback(struct hrtimer *timer)
{
    schedule_work(&workTimeOut);
    return HRTIMER_NORESTART;
}
static struct hrtimer g_timeOutTimer;
static void timerInit(void)
{
  INIT_WORK(&workTimeOut, work_timeOutFunc);
	g_timeOutTimeMs=1000; //1s
	hrtimer_init( &g_timeOutTimer, CLOCK_MONOTONIC, HRTIMER_MODE_REL );
	g_timeOutTimer.function=ledTimeOutCallback;

}



static int sub_strobe_ioctl(unsigned int cmd, unsigned long arg)//(MUINT32 cmd, MUINT32 arg)
{
	int i4RetValue = 0;
	int ior_shift;
	int iow_shift;
	int iowr_shift;
	ior_shift = cmd - (_IOR(FLASHLIGHT_MAGIC,0, int));
	iow_shift = cmd - (_IOW(FLASHLIGHT_MAGIC,0, int));
	iowr_shift = cmd - (_IOWR(FLASHLIGHT_MAGIC,0, int));
	PK_DBG("constant_flashlight_ioctl() line=%d ior_shift=%d, iow_shift=%d iowr_shift=%d arg=%d\n",__LINE__, ior_shift, iow_shift, iowr_shift, (int)arg);
    switch(cmd)
    {

		case FLASH_IOC_SET_TIME_OUT_TIME_MS:
			PK_DBG("FLASH_IOC_SET_TIME_OUT_TIME_MS: %d\n",(int)arg);
			g_timeOutTimeMs=arg;
		break;


    	case FLASH_IOC_SET_DUTY :
    		PK_DBG("FLASHLIGHT_DUTY: %d\n",(int)arg);
    		sub_strobe_dim_duty(arg);
    		break;


    	case FLASH_IOC_SET_STEP:
    		PK_DBG("FLASH_IOC_SET_STEP: %d\n",(int)arg);

    		break;

    	case FLASH_IOC_SET_ONOFF :
    		PK_DBG("FLASHLIGHT_ONOFF: %d\n",(int)arg);
    		if(arg==1)
    		{
				if(g_timeOutTimeMs!=0)
	            {
	            	ktime_t ktime;
					ktime = ktime_set( 0, g_timeOutTimeMs*1000000 );
					hrtimer_start( &g_timeOutTimer, ktime, HRTIMER_MODE_REL );
	            }
    			sub_strobe_Enable();
    			g_strobe_On=1;
    		}
    		else
    		{
    			sub_strobe_Disable();
				hrtimer_cancel( &g_timeOutTimer );
				g_strobe_On=0;
    		}
    		break;
#if 1  //add litianfeng   begin 170210  for sub filllight 补光灯
    	case FLASH_IOC_SET_ON_FILLLIGHT: //210 
    	         PK_DBG("FLASHLIGHT_ONFILLLIGHT sub tianfeng line=%d\n",__LINE__);
    		if(g_strobe_On==0)
    		{
    			sub_strobe_Enable();
    			g_strobe_On=1;
                 PK_DBG("FLASHLIGHT_ONFILLLIGHT sub ON  tianfeng line=%d\n",__LINE__);
    		}

    		break;
    	case FLASH_IOC_SET_OFF_FILLLIGHT : //215
    	        PK_DBG("FLASHLIGHT_OFFFILLLIGHT sub tianfeng line=%d\n",__LINE__);
    		if(g_strobe_On== 1)
    		{

    			sub_strobe_Disable();
    			g_strobe_On=0;
                PK_DBG("FLASHLIGHT_ONFILLLIGHT sub OFF tianfeng line=%d\n",__LINE__);
    		}

    		break;
#endif //add litianfeng   end 170210 for sub filllight 补光灯
		default :
    		PK_DBG(" No such command \n");
    		i4RetValue = -EPERM;
    		break;
    }
    return i4RetValue;
}
#else
static int sub_strobe_ioctl(unsigned int cmd, unsigned long arg)
{
	PK_DBG("sub dummy ioctl");
    return 0;
}
#endif //CONFIG_CUSTOM_KERNEL_SUBFLASHLIGHT


static int sub_strobe_open(void *pArg)
{
#ifdef CONFIG_CUSTOM_KERNEL_SUBFLASHLIGHT
    int i4RetValue = 0;
    PK_DBG("constant_flashlight_open line=%d\n", __LINE__);

	if (0 == strobe_Res)
	{
	    sub_strobe_Init();
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
#else
    PK_DBG("sub dummy open");
    return 0;
#endif
}


static int sub_strobe_release(void *pArg)
{
#ifdef CONFIG_CUSTOM_KERNEL_SUBFLASHLIGHT
    PK_DBG(" constant_flashlight_release\n");

    if (strobe_Res)
    {
        spin_lock_irq(&g_strobeSMPLock);

        strobe_Res = 0;
        strobe_Timeus = 0;

        /* LED On Status */
        g_strobe_On = FALSE;

        spin_unlock_irq(&g_strobeSMPLock);

    	sub_strobe_Uninit();
    }

    PK_DBG(" Done\n");

    return 0;
#else
    PK_DBG("sub dummy release");
    return 0;
#endif
}


FLASHLIGHT_FUNCTION_STRUCT subStrobeFunc = {
	sub_strobe_open,
	sub_strobe_release,
	sub_strobe_ioctl
};


MUINT32 subStrobeInit(PFLASHLIGHT_FUNCTION_STRUCT *pfFunc)
{
    if (pfFunc != NULL)
    {
        *pfFunc = &subStrobeFunc;
    }
    return 0;
}


#ifdef CONFIG_CUSTOM_KERNEL_SUBFLASHLIGHT
EXPORT_SYMBOL(strobe_VDIrq);
#endif

