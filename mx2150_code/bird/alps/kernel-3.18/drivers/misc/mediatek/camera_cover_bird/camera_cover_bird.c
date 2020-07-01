#include <linux/module.h>  
#include <linux/kernel.h>  
#include <linux/fs.h>  
#include <linux/gpio.h>
#include <linux/uaccess.h> 
#include <linux/miscdevice.h>  
#include <linux/device.h>
#include <linux/version.h>
#include <linux/init.h>
#include <linux/interrupt.h>
#include <linux/irq.h>
#include <linux/sched.h>
#include <linux/kthread.h>
#include <linux/pm.h>
#include <linux/sysctl.h>
#include <linux/proc_fs.h>
#include <linux/workqueue.h>
#include <linux/delay.h>
#include <linux/platform_device.h>
#include <linux/input.h> 
#include <mt-plat/aee.h> 
#include <linux/of.h>
#include <linux/of_address.h>
#include <linux/of_irq.h>
#include <mt-plat/upmu_common.h>
#include <mt-plat/mt_boot_common.h>
#include <asm/io.h> 
#include <asm/uaccess.h>


/**********************************************************************************************/
/* configuration*/
/**********************************************************************************************/
#define CAMERA_COVERNAME	"camera_cover"

#define CAMERA_COVER_DEBUG
#if defined(CAMERA_COVER_DEBUG)
#define CAM_TAG		"[CAM_COV] "
#define CAM_FUN(f)		printk(CAM_TAG"%s\n", __FUNCTION__)
#define CAM_ERR(fmt, args...)	printk(CAM_TAG"%s %d : "fmt, __FUNCTION__, __LINE__, ##args)
#define CAM_LOG(fmt, args...)	printk(CAM_TAG fmt, ##args)
#define CAM_DBG(fmt, args...)	printk(CAM_TAG fmt, ##args)    
#else
#define CAM_FUN(f)
#define CAM_ERR(fmt, args...)
#define CAM_LOG(fmt, args...)
#define CAM_DBG(fmt, args...)
#endif
/***********************************************************************************************/
extern int bird_camera_set_shutter;//extern from gc2365mipi_sensor.c
#ifdef CONFIG_BIRD_CAMERA_COVER_SUPPORT_ADD_GC2385
extern int bird_camera_set_shutter_gc2385;//extern from gc2385mipi_sensor.c
#endif
extern int bird_dual_imgsensor_flag;//extern from kd_camera_hw.c
extern int bird_dual_imgsensor_sub_flag;//extern from kd_camera_hw.c

#if defined(CONFIG_SHUTTER_MAPPING_VALUE)
static int bird_camera_set_shutter_value = CONFIG_SHUTTER_MAPPING_VALUE;
#else
static int bird_camera_set_shutter_value = 4000;
#endif
static int bird_camera_set_shutter_temp;
int bird_camera_is_smain_flag = 0;
EXPORT_SYMBOL(bird_camera_is_smain_flag);


static int camera_cover_major;
static struct class *camera_cover_class = NULL;
static struct device *camera_cover_device = NULL;

static const struct of_device_id camera_cover_of_match[] = {
	{
		.compatible = "mediatek,camera_cover"
	},
	{},
};

static struct platform_device camera_cover_dev = {
	.name         = CAMERA_COVERNAME,
	.id       = -1,
	.dev = { 
	},
};

/***********************************************************************************************/
static int camera_cover_open(struct inode *inode, struct file *file)
{
	CAM_FUN(f);
	return 0;	
}

static ssize_t camera_cover_write(struct file *file, const char __user *buf, size_t count, loff_t * ppos)
{
	char val_buf[2];  
	int ret;  
	ret = copy_from_user(val_buf,buf,count);  
	switch(val_buf[0])  
	{  
		case 0x30 :               
			bird_camera_is_smain_flag=0;
			CAM_LOG("bird_camera_is_smain_flag1 =%d,val_buf[0]=%d\n",bird_camera_is_smain_flag,val_buf[0]);//Crystal 
		break;  
		case 0x31 :
			bird_camera_is_smain_flag=1;
			CAM_LOG("bird_camera_is_smain_flag2 =%d,val_buf[0]=%d\n",bird_camera_is_smain_flag,val_buf[0]);//Crystal 	 
                break;  
		default : 
			bird_camera_is_smain_flag=0;
			CAM_LOG("bird_camera_is_smain_flag3 =%d,val_buf[0]=%d\n",bird_camera_is_smain_flag,val_buf[0]);//Crystal 
		break;  
          }  
	
	return 0;
}

/***********************************************************************************************/
static ssize_t  camera_cover_read(struct file *file, char __user *buf, size_t size, loff_t *ppos)
{
	unsigned char state[2];
	int ret;  
	CAM_FUN(f);
	bird_camera_set_shutter_temp=bird_camera_set_shutter;
	CAM_LOG("bird_camera_set_shutter_temp =%d,bird_dual_imgsensor_flag=%d, bird_dual_imgsensor_sub_flag=%d\n",\
		bird_camera_set_shutter_temp,bird_dual_imgsensor_flag,bird_dual_imgsensor_sub_flag);//Crystal
#ifdef CONFIG_BIRD_CAMERA_COVER_SUPPORT_ADD_GC2385
	CAM_LOG("bird_camera_set_shutter_gc2385 =%d,bird_dual_imgsensor_flag=%d, bird_dual_imgsensor_sub_flag=%d\n",\
		bird_camera_set_shutter_gc2385,bird_dual_imgsensor_flag,bird_dual_imgsensor_sub_flag);//Crystal
	if((bird_camera_set_shutter_temp>bird_camera_set_shutter_value||bird_camera_set_shutter_gc2385>bird_camera_set_shutter_value)&&(1==bird_dual_imgsensor_flag)&&(1==bird_dual_imgsensor_sub_flag))
#else
	if((bird_camera_set_shutter_temp>bird_camera_set_shutter_value)&&(1==bird_dual_imgsensor_flag)&&(1==bird_dual_imgsensor_sub_flag))
#endif
		state[0]=1;
	else
		state[0]=0;

	ret=copy_to_user(buf, state, sizeof(state));
	
	return sizeof(state);
}

/***********************************************************************************************/
static struct file_operations camera_cover_fops = {
	.owner  =   THIS_MODULE,    
	.open   =   camera_cover_open,     
	.read	=camera_cover_read,
	.write	=camera_cover_write,	   
};

static ssize_t camera_get_shutter(struct device *dev, struct device_attribute *attr, char *buf)
{

	return sprintf(buf, "%d\n", bird_camera_set_shutter_temp);
}

static ssize_t camera_set_shutter(struct device *dev, struct device_attribute *attr, const char *buf, size_t count)
{
	if(1 == sscanf(buf, "%d", &bird_camera_set_shutter_temp))
	{
		printk("set sensor_shutter to %d\n", bird_camera_set_shutter_temp);
	}
	else
	{
		printk("invalid format = '%s'\n", buf);
	}
	return count;
}

static DEVICE_ATTR(shutter, S_IRUGO|S_IWUSR, camera_get_shutter, camera_set_shutter);

static ssize_t camera_get_values(struct device *dev, struct device_attribute *attr, char *buf)
{

	return sprintf(buf, "%d\n", bird_camera_set_shutter_value);
}

static ssize_t camera_set_values(struct device *dev, struct device_attribute *attr, const char *buf, size_t count)
{
	if(1 == sscanf(buf, "%d", &bird_camera_set_shutter_value))
	{
		printk("set sensor_values to %d\n", bird_camera_set_shutter_value);
	}
	else
	{
		printk("invalid format = '%s'\n", buf);
	}
	return count;
}

static DEVICE_ATTR(values, S_IRUGO|S_IWUSR, camera_get_values, camera_set_values);


/***********************************************************************************************/
static int camera_cover_probe(struct platform_device *pdev)
{       
	CAM_FUN(f);    
	camera_cover_major = register_chrdev(0,CAMERA_COVERNAME,&camera_cover_fops);
    	if (camera_cover_major < 0) {
        	CAM_ERR("Unable to register chardev failed=%d\n",  camera_cover_major);
       		 goto camera_cover_probe_error;
	}
	camera_cover_class = class_create(THIS_MODULE, CAMERA_COVERNAME);  
    	if(NULL == camera_cover_class){
		 CAM_ERR("class_create fail\n");
       		 goto camera_cover_probe_error;
    	}
	camera_cover_device = device_create(camera_cover_class,NULL,MKDEV(camera_cover_major,0),NULL,CAMERA_COVERNAME); 
    	if(NULL == camera_cover_device){
		 CAM_ERR("device_create fail\n");
       		 goto camera_cover_probe_error;
    	}
         if (device_create_file(&pdev->dev, &dev_attr_shutter) < 0)
        {
		printk("Failed to create device shutter file!\n");
                 goto camera_cover_probe_error;
        }
         if (device_create_file(&pdev->dev, &dev_attr_values) < 0)
        {
		printk("Failed to create device values file!\n");
                goto camera_cover_probe_error1;     
        }
	return 0;
camera_cover_probe_error1:
        device_remove_file(&pdev->dev, &dev_attr_shutter);
camera_cover_probe_error:
	unregister_chrdev(camera_cover_major,CAMERA_COVERNAME);  
	device_destroy(camera_cover_class,MKDEV(camera_cover_major,0));  
	class_destroy(camera_cover_class); 
    	return -1;
}
/***********************************************************************************************/
static int camera_cover_remove(struct platform_device *pdev)
{	
	CAM_FUN(f);
        device_remove_file(&pdev->dev, &dev_attr_shutter);
        device_remove_file(&pdev->dev, &dev_attr_values);
	unregister_chrdev(camera_cover_major,CAMERA_COVERNAME);  
	device_destroy(camera_cover_class,MKDEV(camera_cover_major,0));  
	class_destroy(camera_cover_class);  
	return 0;
}
/***********************************************************************************************/
struct platform_driver camera_cover_drv = {
	.probe		= camera_cover_probe,
	.remove		= camera_cover_remove,
	.driver		= {
		.name	= CAMERA_COVERNAME,
		.owner = THIS_MODULE,
		.of_match_table = camera_cover_of_match,
	}
};
/***********************************************************************************************/
static int camera_cover_drv_init(void)
{
	int ret;
	CAM_FUN(f);
	ret = platform_device_register(&camera_cover_dev);
	if (ret)
	{
		CAM_ERR("camera_cover_platform_device_register failed\n");
		return ret;
	}
	ret = platform_driver_register(&camera_cover_drv);
	if (ret)
	{
		CAM_ERR("camera_cover_platform_driver_register failed\n");
		return ret;
	}
	return 0;
}
/***********************************************************************************************/
static void camera_cover_drv_exit(void)
{ 
	CAM_FUN(f);
	platform_driver_unregister(&camera_cover_drv);
	platform_device_unregister(&camera_cover_dev);

}

module_init(camera_cover_drv_init);
module_exit(camera_cover_drv_exit);
MODULE_LICENSE("GPL");
