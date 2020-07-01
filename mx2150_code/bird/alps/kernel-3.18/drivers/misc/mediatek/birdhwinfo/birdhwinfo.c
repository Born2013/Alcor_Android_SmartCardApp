#include <linux/init.h>
#include <linux/module.h>
#include <linux/delay.h>
#include <linux/i2c.h>
#include <linux/input.h>
#include <linux/slab.h>
#include <linux/gpio.h>
#include <linux/sched.h>
#include <linux/kthread.h>
#include <linux/bitops.h>
#include <linux/kernel.h>
#include <linux/delay.h>
#include <linux/byteorder/generic.h>
#ifdef CONFIG_HAS_EARLYSUSPEND
#include <linux/earlysuspend.h>
#endif 
#include <linux/interrupt.h>
#include <linux/time.h>
//#include <linux/rtpm_prio.h>
#include <linux/proc_fs.h>
#include <asm/uaccess.h>
#include <linux/jiffies.h>
#include <linux/platform_device.h>
#include <linux/cdev.h>

//#include <mach/mt_boot.h>

#define maxlenth  255

typedef struct {
	//int id;
	char name[maxlenth + 1];
}BDHWINFO_DATA;

#define BDHWINFO   0x90
#define  BDLCMIC          _IOR(BDHWINFO,0,BDHWINFO_DATA)
#define  BDTPIC           _IOR(BDHWINFO,1,BDHWINFO_DATA)
#define  BDBACKCAMERAIC   _IOR(BDHWINFO,2,BDHWINFO_DATA)
#define  BDFRONTCAMERAIC  _IOR(BDHWINFO,3,BDHWINFO_DATA)

#define ALLOC_DEVNO

static int debug_enable = 1;
#define birdhwinfo_DEBUG(format, args...) do{ \
	if(debug_enable) \
	{\
		printk(KERN_EMERG format,##args);\
	}\
}while(0)

/******************************************************************************/


/* device name and major number */
#define birdhwinfo_DEVNAME            "birdhwinfo"

extern char mtkfb_lcm_name[256];

extern char mtkfb_tp_name[256];
extern char mtkfb_frontcam_name[256];
extern char mtkfb_backcam_name[256];


static BDHWINFO_DATA curlcmic,curtpic,curbackcameraic,curfrontcameraic;

static int birdhwinfo_open(struct inode *inode, struct file *file)
{	
	birdhwinfo_DEBUG("zxw birdhwinfo_open\n");
	return 0;
}

ssize_t birdhwinfo_read(struct file *file, char __user *buf, size_t size, loff_t *ppos)
{
       return 0;
}
static long birdhwinfo_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
	int ret=0;
	switch (cmd)
	{
		case BDLCMIC :
		{	
		

			birdhwinfo_DEBUG("zxw mtkfb_lcm_name[0]=%c,mtkfb_lcm_name[1]=%c,mtkfb_lcm_name=%s\n",mtkfb_lcm_name[0],mtkfb_lcm_name[1],mtkfb_lcm_name);
			strcpy(curlcmic.name,mtkfb_lcm_name);


			

			if (copy_to_user((BDHWINFO_DATA __user *) arg, &curlcmic, sizeof(BDHWINFO_DATA)))
			{
				birdhwinfo_DEBUG("zxw lcm ic copy_to_user err\n");
				ret = -1;
			}
			break;
		}

		case BDTPIC :
		{
			strcpy(curtpic.name,mtkfb_tp_name);
			if (copy_to_user((BDHWINFO_DATA __user *) arg, &curtpic, sizeof(BDHWINFO_DATA)))	
			{
				birdhwinfo_DEBUG("zxw tp ic copy_from_user err\n");
				ret = -1;
			}
			break;
		}

		case BDBACKCAMERAIC :
		{
			birdhwinfo_DEBUG("zxw mtkfb_backcam_name=%s\n",mtkfb_backcam_name);
			strcpy(curbackcameraic.name,mtkfb_backcam_name);
			if (copy_to_user((BDHWINFO_DATA __user *) arg, &curbackcameraic, sizeof(BDHWINFO_DATA))) 
			{
				birdhwinfo_DEBUG("zxw back camera ic copy_from_user err\n");
				ret = -1;
			}
			break;
		}
		
		case BDFRONTCAMERAIC :
		{
			birdhwinfo_DEBUG("zxw mtkfb_backcam_name=%s\n",mtkfb_frontcam_name);
			strcpy(curfrontcameraic.name,mtkfb_frontcam_name);	
			if (copy_to_user((BDHWINFO_DATA __user *) arg, &curfrontcameraic, sizeof(BDHWINFO_DATA))) 
			{
				birdhwinfo_DEBUG("zxw front camera ic copy_from_user err\n");
				ret = -1;
			}
			break;
		}

	}
	return 0;
}

static struct file_operations birdhwinfo_fops = {
    .owner   = THIS_MODULE,
	.open	= birdhwinfo_open,
	.read	 = birdhwinfo_read,
	.unlocked_ioctl  = birdhwinfo_ioctl,
};

static struct class *birdhwinfo_class = NULL;
static struct device *birdhwinfo_device = NULL;
static dev_t birdhwinfo_devno;
static struct cdev birdhwinfo_cdev;

//static struct task_struct *thread = NULL;


static int __init birdhwinfo_probe(struct platform_device *pdev)
{
	int ret = 0, err = 0;

	//int retval = 0;

#ifdef ALLOC_DEVNO
    ret = alloc_chrdev_region(&birdhwinfo_devno, 0, 1, birdhwinfo_DEVNAME);
    if (ret) {
        birdhwinfo_DEBUG("zxw alloc_chrdev_region fail: %d\n", ret);
        goto birdhwinfo_probe_error;
    } else {
        birdhwinfo_DEBUG("zxw major: %d, minor: %d\n", MAJOR(birdhwinfo_devno), MINOR(birdhwinfo_devno));
    }
    cdev_init(&birdhwinfo_cdev, &birdhwinfo_fops);
    birdhwinfo_cdev.owner = THIS_MODULE;
    err = cdev_add(&birdhwinfo_cdev, birdhwinfo_devno, 1);
    if (err) {
        birdhwinfo_DEBUG("zxw cdev_add fail: %d\n", err);
        goto birdhwinfo_probe_error;
    }
#else
    #define birdhwinfo_MAJOR 242
    ret = register_chrdev(birdhwinfo_MAJOR, birdhwinfo_DEVNAME, &birdhwinfo_fops);
    if (ret != 0) {
        birdhwinfo_DEBUG("zxw Unable to register chardev on major=%d (%d)\n", birdhwinfo_MAJOR, ret);
        return ret;
    }
    birdhwinfo_devno = MKDEV(birdhwinfo_MAJOR, 0);
#endif


	birdhwinfo_class = class_create(THIS_MODULE, "birdhwinfo_drv");
    if (IS_ERR(birdhwinfo_class)) {
        birdhwinfo_DEBUG("zxw Unable to create class, err = %d\n", (int)PTR_ERR(birdhwinfo_class));
        goto birdhwinfo_probe_error;
    }

    birdhwinfo_device = device_create(birdhwinfo_class, NULL, birdhwinfo_devno, NULL, birdhwinfo_DEVNAME);

    if(NULL == birdhwinfo_device){
        birdhwinfo_DEBUG("zxw device_create fail\n");
        goto birdhwinfo_probe_error;
    }
    birdhwinfo_DEBUG("zxw birdhwinfo Done\n");
    return 0;

birdhwinfo_probe_error:
#ifdef ALLOC_DEVNO
    if (err == 0)
        cdev_del(&birdhwinfo_cdev);
    if (ret == 0)
        unregister_chrdev_region(birdhwinfo_devno, 1);
#else
    if (ret == 0)
        unregister_chrdev(MAJOR(birdhwinfo_devno), birdhwinfo_DEVNAME);
#endif
    return -1;
}

static int birdhwinfo_remove(struct platform_device *dev)
{


#ifdef ALLOC_DEVNO
    cdev_del(&birdhwinfo_cdev);
    unregister_chrdev_region(birdhwinfo_devno, 1);
#else
    unregister_chrdev(MAJOR(birdhwinfo_devno), birdhwinfo_DEVNAME);
#endif
    device_destroy(birdhwinfo_class, birdhwinfo_devno);
    class_destroy(birdhwinfo_class);

    birdhwinfo_DEBUG("[birdhwinfo_remove] Done\n");
    return 0;
}
	
	
static struct platform_driver bdhwinfo_driver = {
	.driver		= {
		.name	= "birdhwinfo",
		.owner	= THIS_MODULE,
	},
	.probe		= birdhwinfo_probe,
	.remove		= birdhwinfo_remove,
};

struct platform_device bdhwinfo_device = {
	.name = "birdhwinfo",
	.id = -1,
};
	
static int __init birdhwinfo_init(void)
{
	int ret;

	birdhwinfo_DEBUG("zxw birdhwinfo_init\n");
	ret = platform_device_register(&bdhwinfo_device);
	if (ret)
		birdhwinfo_DEBUG("[birdhwinfo] zxw birdhwinfo_device_init:dev:%d\n", ret);

	ret = platform_driver_register(&bdhwinfo_driver);


	if (ret)
	{
		birdhwinfo_DEBUG("[birdhwinfo]birdhwinfo_init:drv:E%d\n", ret);
		return ret;
	}

	return ret;
}


static void __exit birdhwinfo_exit(void)
{
	platform_driver_unregister(&bdhwinfo_driver);
}



module_param(debug_enable, int,0644);

module_init(birdhwinfo_init);
module_exit(birdhwinfo_exit);

MODULE_AUTHOR("Bird Inc By zxw");
MODULE_DESCRIPTION("BIRDHWINFO for MediaTek MT65xx chip");
MODULE_LICENSE("GPL");
MODULE_ALIAS("BIRDHWINFO");
