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


#include "../imgsensor/inc/kd_camera_typedef.h"
#include "../imgsensor/inc/kd_imgsensor.h"
#include "../imgsensor/inc/kd_imgsensor_define.h"
#include "../imgsensor/inc/kd_imgsensor_errcode.h"
#include "../imgsensor/inc/kd_camera_feature.h"
#ifdef CONFIG_ARCH_MT6735
#include "../imgsensor/src/mt6735/camera_hw/kd_camera_hw.h"
#else
#include "../imgsensor/src/mt6735m/camera_hw/kd_camera_hw.h"
#endif

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

extern int bird_main_camera_ready;//extern from kd_camera_hw.c

#if 1//def CONFIG_BIRD_CAMERA_COVER_SUPPORT_GC2145_FOR_SMT
extern kal_uint16 bird_sensor_id_gc2145_for_cover;
#endif

#if 0//defined(CONFIG_SHUTTER_MAPPING_VALUE)
static int Yaverage_Threshold = CONFIG_SHUTTER_MAPPING_VALUE;
#else
static int Yaverage_Threshold = 20;
#endif

#ifndef CONFIG_BIRD_CAMERA_COVER_SUPPORT_GC2145_FOR_SMT
static int Shutter_Threshold = 1000;
static int Analog_gain_Threshold = 100;
#endif

static int Yaverage_Temp;


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

extern int mtkcam_gpio_set(int PinIdx, int PwrType, int Val); //from kd_camera_hw.c
extern UINT32 GC2145MIPIOpen(void);//from gc2145mipi_yuv_Sensor
extern UINT32 GC2145MIPIClose(void);//from gc2145mipi_yuv_Sensor
extern void GC2145MIPI_Read_Information(kal_uint16 *Shutter, kal_uint8 *Analog_gain, kal_uint8 *Yaverage);//from gc2145mipi_yuv_Sensor
static int camera_cover_is_opened = 0;

static ssize_t camera_cover_write(struct file *file, const char __user *buf, size_t count, loff_t * ppos)
{
	char val_buf[2];  
	int ret;  
	kal_uint16 bird_sensor_id_gc2145_for_cover_temp=0;
	ret = copy_from_user(val_buf,buf,count);  
	switch(val_buf[0])  
	{  
		case 0x30 :  //close gc2145
			GC2145MIPIClose();
			mtkcam_gpio_set(1, CAM1PDN, 1);
			mtkcam_gpio_set(1, CAM1RST, 0);	
			mdelay(5);		
			//_hwPowerDown(VCAMIO);//power in kd_camera_hw
			//_hwPowerDown(VCAMA);//power in kd_camera_hw
			//mdelay(5);
			camera_cover_is_opened = 0;
			CAM_LOG("GC2145MIPIClose\n");//Crystal          
		break;  

		case 0x31 :  //open gc2145
		if(bird_main_camera_ready==1){
			CAM_LOG("bird_main_camera_is ready\n");//Crystal 

			mtkcam_gpio_set(1, CAM1PDN, 1);
			mtkcam_gpio_set(1, CAM1RST, 0);
			mdelay(5);
			/*VCAMA*/
			//_hwPowerOn(VCAMA, 2800000);//power in kd_camera_hw
			//mdelay(5);
			/*VCAMIO*/
			//_hwPowerOn(VCAMIO, 1800000);//power in kd_camera_hw
			//mdelay(5);
			mtkcam_gpio_set(1, CAM1RST, 0);
			mdelay(5);
			mtkcam_gpio_set(1, CAM1RST, 1);
	
			mtkcam_gpio_set(1, CAM1PDN,1);
			mdelay(5);
			mtkcam_gpio_set(1, CAM1PDN,0);
			mdelay(5);

			GC2145MIPIOpen();
			bird_sensor_id_gc2145_for_cover_temp = bird_sensor_id_gc2145_for_cover;
			CAM_LOG("bird_sensor_id_gc2145_for_cover_temp=%d\n",bird_sensor_id_gc2145_for_cover_temp);//Crystal 

			if(bird_sensor_id_gc2145_for_cover_temp==0x2145){
				camera_cover_is_opened = 1;
			}else{
				camera_cover_is_opened = 0;
			}
		}
		else{
			CAM_LOG("bird_main_camera_is not ready\n");//Crystal 
			camera_cover_is_opened = 0;
		}
			mdelay(10);
			CAM_LOG("GC2145MIPIOpen\n");//Crystal 	 
                break;
  
		default :  //close gc2145  	
		break;  
          }  
	
	return 0;
}

/***********************************************************************************************/
#ifndef CONFIG_BIRD_CAMERA_COVER_SUPPORT_GC2145_FOR_SMT
static ssize_t  camera_cover_read(struct file *file, char __user *buf, size_t size, loff_t *ppos)
{
	unsigned char state[1]; 
	int ret;  
	kal_uint16 Shutter=0;
 	kal_uint8 Analog_gain=0;
	kal_uint8 Yaverage=0;
	CAM_FUN(f);

	if(1==camera_cover_is_opened)
	GC2145MIPI_Read_Information(&Shutter, &Analog_gain, &Yaverage);
	CAM_LOG("Shutter=0x%x,Analog_gain=0x%x,Yaverage=0x%x\n",Shutter,Analog_gain,Yaverage);
	CAM_LOG("Shutter=%d,Analog_gain=%d,Yaverage=%d\n",Shutter,Analog_gain,Yaverage);

	Yaverage_Temp = Yaverage;

	if((Shutter > Shutter_Threshold)&&(Analog_gain > Analog_gain_Threshold)&&(Yaverage < Yaverage_Threshold))
		state[0]=1;
	else
		state[0]=0;

	ret=copy_to_user(buf, state, sizeof(state));
	
	return sizeof(state);
}
#else
static ssize_t  camera_cover_read(struct file *file, char __user *buf, size_t size, loff_t *ppos)
{
	unsigned char state[1]; 
	int ret;  
	kal_uint16 bird_sensor_id_gc2145_for_cover_temp=0;
	CAM_FUN(f);

	mtkcam_gpio_set(1, CAM1PDN, 1);
	mtkcam_gpio_set(1, CAM1RST, 0);
	mdelay(5);
	/*VCAMA*/
	//_hwPowerOn(VCAMA, 2800000);//power in kd_camera_hw
	//mdelay(5);
	/*VCAMIO*/
	//_hwPowerOn(VCAMIO, 1800000);//power in kd_camera_hw
	//mdelay(5);
	mtkcam_gpio_set(1, CAM1RST, 0);
	mdelay(5);
	mtkcam_gpio_set(1, CAM1RST, 1);
	
	mtkcam_gpio_set(1, CAM1PDN,1);
	mdelay(5);
	mtkcam_gpio_set(1, CAM1PDN,0);
	mdelay(5);

	GC2145MIPIOpen();

	bird_sensor_id_gc2145_for_cover_temp = bird_sensor_id_gc2145_for_cover;
	CAM_LOG("bird_sensor_id_gc2145_for_cover_temp=%d\n",bird_sensor_id_gc2145_for_cover_temp);//Crystal 

	GC2145MIPIClose();
	mtkcam_gpio_set(1, CAM1PDN, 1);
	mtkcam_gpio_set(1, CAM1RST, 0);	
	mdelay(5);		
	//_hwPowerDown(VCAMIO);//power in kd_camera_hw
	//_hwPowerDown(VCAMA);//power in kd_camera_hw
	//mdelay(5);
	CAM_LOG("GC2145MIPIClose\n");//Crystal 

	if(bird_sensor_id_gc2145_for_cover_temp==0x2145)
		state[0]=1;
	else
		state[0]=0;

	ret=copy_to_user(buf, state, sizeof(state));
	
	return sizeof(state);
}
#endif

/***********************************************************************************************/
static struct file_operations camera_cover_fops = {
	.owner  =   THIS_MODULE,    
	.open   =   camera_cover_open,     
	.read	=  camera_cover_read,
	.write	=  camera_cover_write,	   
};

static ssize_t camera_get_shutter(struct device *dev, struct device_attribute *attr, char *buf)
{

	return sprintf(buf, "%d\n", Yaverage_Temp);
}

static ssize_t camera_set_shutter(struct device *dev, struct device_attribute *attr, const char *buf, size_t count)
{
	if(1 == sscanf(buf, "%d", &Yaverage_Temp))
	{
		printk("set sensor_shutter to %d\n", Yaverage_Temp);
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

	return sprintf(buf, "%d\n", Yaverage_Threshold);
}

static ssize_t camera_set_values(struct device *dev, struct device_attribute *attr, const char *buf, size_t count)
{
	if(1 == sscanf(buf, "%d", &Yaverage_Threshold))
	{
		printk("set sensor_values to %d\n", Yaverage_Threshold);
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

