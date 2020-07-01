/* Copyright (C) Microarray
 * MicroArray Fprint Driver Code for TEE enviroment
 * madev_tee.c
 * Date: 2016-11-17
 * Version: v4.0.03 
 * Author: guq
 * Contact: guq@microarray.com.cn
 */
#include "madev.h"


//spdev use for recording the data for other use
static unsigned int irq, ret;
static unsigned int is_enable_irq = -1;
static unsigned int u1_flag = 0;
static unsigned int u2_flag = 0;
static int irq_flag = 0;
static unsigned int ma_drv_reg;
static struct spi_device *ma_spi_t;
static DECLARE_WAIT_QUEUE_HEAD(gWaitq);
static DECLARE_WAIT_QUEUE_HEAD(U1_Waitq);
static DECLARE_WAIT_QUEUE_HEAD(U2_Waitq);
struct wake_lock gIntWakeLock;
struct wake_lock gProcessWakeLock;
struct work_struct gWork;
struct workqueue_struct *gWorkq;
static struct fprint_dev * sdev;
static struct input_dev * ma_input_t;
//
static LIST_HEAD(dev_list);
static DEFINE_MUTEX(dev_lock);
static DEFINE_MUTEX(drv_lock);
static DEFINE_MUTEX(ioctl_lock);
static DECLARE_WAIT_QUEUE_HEAD(drv_waitq);
//

static void mas_work(struct work_struct *pws) {
    irq_flag = 1;
    wake_up_interruptible(&gWaitq);
}

static irqreturn_t mas_interrupt(int irq, void *dev_id) {
    wake_lock_timeout(&gIntWakeLock, 5*HZ);
    queue_work(gWorkq, &gWork);
    return IRQ_HANDLED;
}


static void mas_set_input(void) {
    struct input_dev *input = NULL;
    input = input_allocate_device();
    if (!input) {
        MALOGW("input_allocate_device failed.");
        return ;
    }
    set_bit(EV_KEY, input->evbit);
    set_bit(EV_ABS, input->evbit);
    set_bit(EV_SYN, input->evbit);
    set_bit(FINGERPRINT_SWIPE_UP, input->keybit); //单触
    set_bit(FINGERPRINT_SWIPE_DOWN, input->keybit);
    set_bit(FINGERPRINT_SWIPE_LEFT, input->keybit);
    set_bit(FINGERPRINT_SWIPE_RIGHT, input->keybit);
    set_bit(FINGERPRINT_TAP, input->keybit);
    set_bit(FINGERPRINT_DTAP, input->keybit);
    set_bit(FINGERPRINT_LONGPRESS, input->keybit);
    
    set_bit(KEY_POWER, input->keybit);

    input->name = MA_CHR_DEV_NAME;
    input->id.bustype = BUS_SPI;
    ret = input_register_device(input);
    if (ret) {
        input_free_device(input);
        MALOGW("failed to register input device.");
        return;
    }
    ma_input_t  = input;
}



//static int mas_ioctl (struct inode *node, struct file *filp, unsigned int cmd, uns igned long arg)           
//this function only supported while the linux kernel version under v2.6.36,while the kernel version under v2.6.36, use this line
static long mas_ioctl(struct file *filp, unsigned int cmd, unsigned long arg) {
    //MALOGF("start");
    switch(cmd){
        case MA_IOC_DELK:                                                       //延时锁    timeout lock
            wake_lock_timeout(&gProcessWakeLock, 5*HZ);
            break;
        case MA_IOC_SLEP:                                                       //remove the process out of the runqueue
            irq_flag = 0;
            wait_event_interruptible(gWaitq, irq_flag != 0);
            break;
        case MA_IOC_WKUP:                                                       //wake up, schedule the process into the runqueue
            irq_flag = 1;
            wake_up_interruptible(&gWaitq);
            break;
        case MA_IOC_ENCK:
            mas_enable_spi_clock(ma_spi_t);                                    //if the spi clock is not opening always, do this methods
            break;
        case MA_IOC_DICK:
            mas_disable_spi_clock(ma_spi_t);                                   //disable the spi clock
            break;
        case MA_IOC_EINT:
	    if(is_enable_irq <= 0){
	    	MALOGW("do request irq first!\n");
		ret = request_irq(irq, mas_interrupt, IRQF_TRIGGER_RISING, MA_EINT_NAME, NULL);
		if(ret <= 0){
			MALOGE("request_irq");
			break;
		}
		is_enable_irq = ret;
		break;
	    }
            enable_irq(irq);                                                    //enable the irq,in fact, you can make irq enable always
            break;
        case MA_IOC_DINT:
	    if(is_enable_irq <= 0){
		MALOGW("do request irq first!\n");
		ret = request_irq(irq, mas_interrupt, IRQF_TRIGGER_RISING, MA_EINT_NAME, NULL);
		if(ret <= 0){
			MALOGE("request_irq");
			break;
		}
		is_enable_irq = ret;
		break;
	    }
            disable_irq(irq);                                                    //disable the irq
            break;
        case MA_IOC_TPDW:
            input_report_key(ma_input_t, FINGERPRINT_TAP, 1);
            input_sync(ma_input_t);                                                 //tap down
            break;
        case MA_IOC_TPUP:
            input_report_key(ma_input_t, FINGERPRINT_TAP, 0);
            input_sync(ma_input_t);                                                     //tap up
            break;
        case MA_IOC_SGTP:
            input_report_key(ma_input_t, FINGERPRINT_TAP, 1);
            input_sync(ma_input_t);
            input_report_key(ma_input_t, FINGERPRINT_TAP, 0);
            input_sync(ma_input_t);                                                       //single tap
            break;
        case MA_IOC_DBTP:
            input_report_key(ma_input_t, FINGERPRINT_DTAP, 1);
            input_sync(ma_input_t);
            input_report_key(ma_input_t, FINGERPRINT_DTAP, 0);
            input_sync(ma_input_t);                                              //double tap
            break;
        case MA_IOC_LGTP:
            input_report_key(ma_input_t, FINGERPRINT_LONGPRESS, 1);
            input_sync(ma_input_t);
            input_report_key(ma_input_t, FINGERPRINT_LONGPRESS, 0);
            input_sync(ma_input_t);                                               //long tap
            break;
	case MA_IOC_EIRQ:		//request irq,if define the TEE_COMPATIBLE,the driver cannot get the chip id,so only one driver can get the request,here ca must request the irq through the ioctl interface
	    ret = request_irq(irq, mas_interrupt, IRQF_TRIGGER_RISING, MA_EINT_NAME, NULL);
	    if(ret <= 0){
	    	MALOGE("request irq");
	    	break;
	    }
	    is_enable_irq = ret;
	    break;
  	case MA_IOC_DIRQ:
	    if(is_enable_irq > 0){
	    	free_irq(irq, NULL);
	    }
	    break;
	case MA_IOC_NAVW:
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_UP, 1);
	    input_sync(ma_input_t);
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_UP, 0);
	    input_sync(ma_input_t);
	    break;
	case MA_IOC_NAVA:
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_LEFT, 1);
	    input_sync(ma_input_t);
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_LEFT, 0);
	    input_sync(ma_input_t);
	    break;
	case MA_IOC_NAVS:
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_DOWN, 1);
	    input_sync(ma_input_t);
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_DOWN, 0);
	    input_sync(ma_input_t);
	    break;
	case MA_IOC_NAVD:
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_RIGHT, 1);
	    input_sync(ma_input_t);
	    input_report_key(ma_input_t, FINGERPRINT_SWIPE_RIGHT, 0);
	    input_sync(ma_input_t);
	    break;
	case MA_IOC_SPAR:
	    mutex_lock(&ioctl_lock);
	    ret = copy_from_user(&ma_drv_reg, (unsigned int*)arg, sizeof(unsigned int));
	    mutex_unlock(&ioctl_lock);
	    break;
	case MA_IOC_GPAR:
	    //TODO
	    mutex_lock(&ioctl_lock);
	    ret = copy_to_user((unsigned int*)arg, &ma_drv_reg, sizeof(unsigned int));
	    mutex_unlock(&ioctl_lock);
	    break;
	case MA_IOC_GVER:
	    mutex_lock(&ioctl_lock);
	    *((unsigned int*)arg) = MA_DRV_VERSION;
	    mutex_unlock(&ioctl_lock);
	    break;
    case MA_IOC_STSP:
        ret = copy_from_user(&ma_speed, (unsigned int*)arg, sizeof(unsigned int));
        ma_spi_change(smas->spi, ma_speed, 0);
        break;
	case MA_IOC_FD_WAIT_CMD:
    	u2_flag = 0;
        wait_event_interruptible(U2_Waitq, u2_flag != 0);
        break;
    case MA_IOC_TEST_WAKE_FD:
        u2_flag = 1;
        wake_up_interruptible(&U2_Waitq);
        break;
    case MA_IOC_TEST_WAIT_FD_RET:
		u1_flag = 0;
        wait_event_interruptible(U1_Waitq,  u1_flag != 0);
        mutex_lock(&ioctl_lock);
        ret = copy_to_user((unsigned int*)arg, &ma_drv_reg, sizeof(unsigned int));
        mutex_unlock(&ioctl_lock);
    case MA_IOC_FD_WAKE_TEST_RET:
    	mutex_lock(&ioctl_lock);
        ret = copy_from_user(&ma_drv_reg, (unsigned int*)arg, sizeof(unsigned int));
        mutex_unlock(&ioctl_lock);
        msleep(120);
        u1_flag = 1;
        wake_up_interruptible(&U1_Waitq);
        break;
	default:
            ret = -EINVAL;
            MALOGW("mas_ioctl no such cmd");
    }
    //MALOGF("end");
    return ret;
}

#ifdef CONFIG_COMPAT
static long mas_compat_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
{
    int retval = 0;
    retval = filp->f_op->unlocked_ioctl(filp, cmd, arg);
    return retval;
}
#endif


/*---------------------------------- fops ------------------------------------*/
static const struct file_operations sfops = {
    .owner = THIS_MODULE,
    .unlocked_ioctl = mas_ioctl,
    //.ioctl = mas_ioctl,       
    //using the previous line replacing the unlock_ioctl while the linux kernel under version2.6.36
#ifdef CONFIG_COMPAT
    .compat_ioctl = mas_compat_ioctl,
#endif

};
/*---------------------------------- fops end ---------------------------------*/

static int init_file_node(void)
{
    	int ret;
	//MALOGF("start");
    	ret = alloc_chrdev_region(&sdev->idd, 0, 1, MA_CHR_DEV_NAME);
    	if(ret < 0)
    	{
        	MALOGW("alloc_chrdev_region error!");
        	return -1;
    	}
    	sdev->chd = cdev_alloc();
   	    if (!sdev->chd)
    	{
        	MALOGW("cdev_alloc error!");
        	return -1;
    	}
    	sdev->chd->owner = THIS_MODULE;
    	sdev->chd->ops = &sfops;
    	cdev_add(sdev->chd, sdev->idd, 1);
	    sdev->cls = class_create(THIS_MODULE, MA_CHR_DEV_NAME);
	    if (IS_ERR(sdev->cls)) {
		  MALOGE("class_create");
		  return -1;
	    }
	    sdev->dev = device_create(sdev->cls, NULL, sdev->idd, NULL, MA_CHR_FILE_NAME);
	    ret = IS_ERR(sdev->dev) ? PTR_ERR(sdev->dev) : 0;
	    if(ret){
	       	MALOGE("device_create");
	    }
	    //MALOGF("end");
        return 0;
}

static int deinit_file_node(void)
{
    unregister_chrdev_region(sdev->idd, 1);
    cdev_del(sdev->chd);
    return 0;
}

static int init_interrupt(void)
{
    irq = mas_get_irq();
    if(irq<=0){
        ret = irq;
        MALOGE("mas_get_irq");
    }
#if 0
    ret = request_irq(irq, mas_interrupt, IRQF_TRIGGER_RISING, tname, NULL);
    if(ret<=0){
        MALOGE("request_irq");
    }
    is_enable_irq = ret;
    enable_irq(irq);
#endif
    return ret;
}
static int deinit_interrupt(void)
{
#if 0
    disable_irq(irq);
    free_irq(irq, NULL);
#endif
    return 0;
}


static int init_vars(void)
{
    sdev = kmalloc(sizeof(struct fprint_dev), GFP_KERNEL);
    if(sdev == NULL){
    	MALOGE("malloc sdev space failed!\n");
    }
    ma_drv_reg = 0;
    wake_lock_init(&gIntWakeLock, WAKE_LOCK_SUSPEND,"microarray_int_wakelock");
    wake_lock_init(&gProcessWakeLock, WAKE_LOCK_SUSPEND,"microarray_process_wakelock");
    INIT_WORK(&gWork, mas_work);
    gWorkq = create_singlethread_workqueue("mas_workqueue");
    if (!gWorkq) {
        MALOGW("create_single_workqueue error!");
        return -ENOMEM;
    }
    return 0;
}
static int deinit_vars(void)
{
    destroy_workqueue(gWorkq);
    wake_lock_destroy(&gIntWakeLock);
    wake_lock_destroy(&gProcessWakeLock);
    return 0;
}

static int init_spi(struct spi_device *spi){
    ma_spi_t = spi;
    mas_enable_spi_clock(ma_spi_t);                 //if needed
    return 0;
}

static int deinit_spi(struct spi_device *spi){
    mas_disable_spi_clock(ma_spi_t);
    return 0;
}


int mas_plat_probe(struct platform_device *pdev) {
    //MALOGD("start");
    ret = mas_finger_get_gpio_info(pdev);
    if(ret){
        MALOGE("mas_plat_probe do mas_finger_get_gpio_info");
    }
    ret = mas_finger_set_gpio_info(1);
    if(ret){
        MALOGE("mas_plat_probe do mas_finger_set_gpio_info");
    }
    //MALOGD("end");
    return ret;
}

int mas_plat_remove(struct platform_device *pdev) {
    mas_finger_set_gpio_info(0);
    return 0;
} 


int mas_probe(struct spi_device *spi) {

    //MALOGD("start");
    ret = init_vars(); 
    if(ret){
        goto err1;
    }
    ret = init_interrupt();
    if(ret){
        goto err2;
    }
    ret = init_file_node(); 
    if(ret){
        goto err3;
    }
    ret = init_spi(spi);
    if(ret){
        goto err4;
    }
    mas_set_input();
    MALOGF("end");
    
    return ret;

#if 0
        //if needed 
        ret = init_connect();
        goto err0;
        //end
#endif
err4:
    deinit_spi(spi);
err3:
    deinit_file_node();
err2:
    deinit_interrupt();
err1:
    deinit_vars();


    return 0;
}

int mas_remove(struct spi_device *spi) {
    deinit_file_node();
    deinit_interrupt();
    deinit_vars();
    return 0;
}


static int __init mas_init(void)
{
    int ret = 0;
    MALOGF("start");
    ret = mas_get_platform();
    if(ret){
	   MALOGE("mas_get_platform");
    }
    return ret;
}

static void __exit mas_exit(void)
{
}

module_init(mas_init);
module_exit(mas_exit);

MODULE_AUTHOR("Microarray");
MODULE_DESCRIPTION("Driver for microarray fingerprint sensor");
MODULE_LICENSE("GPL");
