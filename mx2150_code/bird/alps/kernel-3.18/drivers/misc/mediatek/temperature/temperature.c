#include "temperature.h"

struct tp_context *tp_context_obj = NULL;

static struct tp_init_info *tp_init_list[MAX_CHOOSE_TEMPERATURE_NUM] = {0};
static bool tp_misc_dev_init;
static struct platform_device *plt_dev;

static int tp_real_enable(int enable)
{
	int err = 0;
	struct tp_context *cxt = NULL;
        printk("temperature: %s\n",__func__);
	cxt = tp_context_obj;
	if (1 == enable) {
		if (true == cxt->is_tp_active_data || true == cxt->is_tp_active_nodata) {
			err = cxt->tp_ctl.enable_nodata(1);
			if (err)
				err = cxt->tp_ctl.enable_nodata(1);
				if (err) {
					err = cxt->tp_ctl.enable_nodata(1);
					if (err)
						printk("temperature enable(%d) err 3 timers = %d\n", enable, err);
				}
			}
			printk("temperature real enable\n");
	}

	if (0 == enable) {
		if (false == cxt->is_tp_active_data && false == cxt->is_tp_active_nodata) {
				err = cxt->tp_ctl.enable_nodata(0);
				if (err)
					printk("temperature enable(%d) err = %d\n", enable, err);
			printk("temperature real disable\n");
		}
	}

	return err;
}

static int tp_enable_data(int enable)
{
	struct tp_context *cxt = NULL;
        printk("temperature: %s\n",__func__);
	cxt = tp_context_obj;
	if (NULL  == cxt->tp_ctl.open_report_data) {
		printk("no temperature control path\n");
		return -1;
	}

	if (1 == enable) {
		printk("TEMPERATURE enable data\n");
		cxt->is_tp_active_data = true;
		cxt->is_tp_first_data_after_enable = true;
		cxt->tp_ctl.open_report_data(1);
		tp_real_enable(enable);
		if (false == cxt->is_tp_polling_run && cxt->is_tp_batch_enable == false) {
			if (false == cxt->tp_ctl.is_report_input_direct) {
				cxt->is_get_valid_tp_data_after_enable = false;
				mod_timer(&cxt->timer_tp, jiffies + atomic_read(&cxt->delay_tp)/(1000/HZ));
				cxt->is_tp_polling_run = true;
			}
		}
	}

	if (0 == enable) {
		printk("TEMPERATURE disable\n");
		cxt->is_tp_active_data = false;
		cxt->tp_ctl.open_report_data(0);
		if (true == cxt->is_tp_polling_run) {
			if (false == cxt->tp_ctl.is_report_input_direct) {
				cxt->is_tp_polling_run = false;
				smp_mb();/* for memory barrier */
				del_timer_sync(&cxt->timer_tp);
				smp_mb();/* for memory barrier */
				cancel_work_sync(&cxt->report_tp);
			}
		}
		tp_real_enable(enable);
	}
	return 0;
}


static ssize_t tp_store_active(struct device *dev, struct device_attribute *attr,
				  const char *buf, size_t count)
{
	struct tp_context *cxt = NULL;
        printk("temperature: %s\n",__func__);
	printk("temperature_store_active buf=%s\n", buf);
	mutex_lock(&tp_context_obj->tp_op_mutex);
	cxt = tp_context_obj;

	if (!strncmp(buf, "1", 1))
		tp_enable_data(1);
	else if (!strncmp(buf, "0", 1))
		tp_enable_data(0);
	else
		printk(" temperature_store_active error !!\n");

	mutex_unlock(&tp_context_obj->tp_op_mutex);
	printk(" temperature_store_active done\n");
	return count;
}
/*----------------------------------------------------------------------------*/
static ssize_t tp_show_active(struct device *dev,
				 struct device_attribute *attr, char *buf)
{
	struct tp_context *cxt = NULL;
	int div = 0;
        printk("temperature: %s\n",__func__);
	cxt = tp_context_obj;
	div = cxt->tp_data.vender_div;
	printk("temperature vender_div value: %d\n", div);
	return snprintf(buf, PAGE_SIZE, "%d\n", div);
}

static ssize_t tp_store_delay(struct device *dev, struct device_attribute *attr,
				  const char *buf, size_t count)
{
	int delay;
	int mdelay = 0;
	int ret = 0;
	struct tp_context *cxt = NULL;
        printk("temperature: %s\n",__func__);
	mutex_lock(&tp_context_obj->tp_op_mutex);
	cxt = tp_context_obj;
	if (NULL == cxt->tp_ctl.set_delay) {
		printk("temperature_ctl set_delay NULL\n");
		mutex_unlock(&tp_context_obj->tp_op_mutex);
		return count;
	}

	ret = kstrtoint(buf, 10, &delay);
	if (0 != ret) {
		printk("invalid format!!\n");
		mutex_unlock(&tp_context_obj->tp_op_mutex);
		return count;
	}

	if (false == cxt->tp_ctl.is_report_input_direct) {
		mdelay = (int)delay/1000/1000;
		atomic_set(&tp_context_obj->delay_tp, mdelay);
	}
	cxt->tp_ctl.set_delay(delay);
	printk(" temperature_delay %d ns\n", delay);
	mutex_unlock(&tp_context_obj->tp_op_mutex);
	return count;
}

static ssize_t tp_show_delay(struct device *dev,
				 struct device_attribute *attr, char *buf)
{
	int len = 0;
	printk(" not support now\n");
	return len;
}


static ssize_t tp_store_batch(struct device *dev, struct device_attribute *attr,
				  const char *buf, size_t count)
{
	struct tp_context *cxt = NULL;
        printk("temperature: %s\n",__func__);
	printk("temperature_store_batch buf=%s\n", buf);
	mutex_lock(&tp_context_obj->tp_op_mutex);
	cxt = tp_context_obj;
	if (cxt->tp_ctl.is_support_batch) {
		if (!strncmp(buf, "1", 1)) {
			cxt->is_tp_batch_enable = true;
			if (true == cxt->is_tp_polling_run) {
				cxt->is_tp_polling_run = false;
				del_timer_sync(&cxt->timer_tp);
				cancel_work_sync(&cxt->report_tp);
			}
		} else if (!strncmp(buf, "0", 1)) {
			cxt->is_tp_batch_enable = false;
			if (false == cxt->is_tp_polling_run) {
				if (false == cxt->tp_ctl.is_report_input_direct) {
					cxt->is_get_valid_tp_data_after_enable = false;
					mod_timer(&cxt->timer_tp, jiffies + atomic_read(&cxt->delay_tp)/(1000/HZ));
					cxt->is_tp_polling_run = true;
				}
			}
		} else
			printk(" temperature_store_batch error !!\n");
	} else
		printk(" temperature_store_batch not supported\n");

	mutex_unlock(&tp_context_obj->tp_op_mutex);
	printk(" temperature_store_batch done: %d\n", cxt->is_tp_batch_enable);
	return count;
}

static ssize_t tp_show_batch(struct device *dev,
				 struct device_attribute *attr, char *buf)
{
        printk("temperature: %s\n",__func__);
	return snprintf(buf, PAGE_SIZE, "%d\n", 0);
}

static ssize_t tp_show_devnum(struct device *dev,
				 struct device_attribute *attr, char *buf)
{
	unsigned int devnum;
	const char *devname = NULL;
	int ret;
        printk("temperature: %s\n",__func__);
	devname = dev_name(&tp_context_obj->idev->dev);
	ret = sscanf(devname+5, "%d", &devnum);
	return snprintf(buf, PAGE_SIZE, "%d\n", devnum);
}

DEVICE_ATTR(tempactive,		S_IWUSR | S_IRUGO, tp_show_active, tp_store_active);
DEVICE_ATTR(tempdelay,		S_IWUSR | S_IRUGO, tp_show_delay,  tp_store_delay);
DEVICE_ATTR(tempbatch,		S_IWUSR | S_IRUGO, tp_show_batch,  tp_store_batch);
DEVICE_ATTR(tempdevnum,		S_IWUSR | S_IRUGO, tp_show_devnum,  NULL);


static struct attribute *tp_attributes[] = {
	&dev_attr_tempactive.attr,
	&dev_attr_tempdelay.attr,
	&dev_attr_tempbatch.attr,
	&dev_attr_tempdevnum.attr,
	NULL
};

static struct attribute_group tp_attribute_group = {
	.attrs = tp_attributes
};

int tp_register_data_path(struct tp_data_path *data)
{
	struct tp_context *cxt = NULL;
        printk("temperature: %s\n",__func__);
	cxt = tp_context_obj;
	cxt->tp_data.get_data = data->get_data;
	cxt->tp_data.vender_div = data->vender_div;
	//cxt->tp_data.tp_get_raw_data = data->tp_get_raw_data;
	printk("temperature register data path vender_div: %d\n", cxt->tp_data.vender_div);
	if (NULL == cxt->tp_data.get_data) {
		printk("tp register data path fail\n");
		return -1;
	}
	return 0;
}

static int tp_misc_init(struct tp_context *cxt)
{
	int err = 0;
        printk("temperature: %s\n",__func__);
	cxt->mdev.minor = MISC_DYNAMIC_MINOR;
	cxt->mdev.name  = TEMPERATURE_MISC_DEV_NAME;
	err = misc_register(&cxt->mdev);
	if (err)
		printk("unable to register temperature misc device!!\n");

	return err;
}

int tp_register_control_path(struct tp_control_path *ctl)
{
	struct tp_context *cxt = NULL;
	int err = 0;
        printk("temperature: %s\n",__func__);
	cxt = tp_context_obj;
	cxt->tp_ctl.set_delay = ctl->set_delay;
	cxt->tp_ctl.open_report_data = ctl->open_report_data;
	cxt->tp_ctl.enable_nodata = ctl->enable_nodata;
	cxt->tp_ctl.is_support_batch = ctl->is_support_batch;
	cxt->tp_ctl.is_report_input_direct = ctl->is_report_input_direct;
	cxt->tp_ctl.is_use_common_factory = ctl->is_use_common_factory;

	if (NULL == cxt->tp_ctl.set_delay || NULL == cxt->tp_ctl.open_report_data
		|| NULL == cxt->tp_ctl.enable_nodata) {
		printk("temperature register control path fail\n");
		return -1;
	}

	if (!tp_misc_dev_init) {
		/* add misc dev for sensor hal control cmd */
		err = tp_misc_init(tp_context_obj);
		if (err) {
			printk("unable to register temperature misc device!!\n");
			return -2;
		}
		err = sysfs_create_group(&tp_context_obj->mdev.this_device->kobj,
				&tp_attribute_group);
		if (err < 0) {
			printk("unable to create temperature attribute file\n");
			return -3;
		}
		kobject_uevent(&tp_context_obj->mdev.this_device->kobj, KOBJ_ADD);
		tp_misc_dev_init = true;
	}
	return 0;
}

int tp_data_report(struct input_dev *dev, int value)
{
	struct tp_context *cxt = NULL;
        printk("temperature: %s\n",__func__);
	cxt  = tp_context_obj;
	if (cxt->is_get_valid_tp_data_after_enable == false) {
		cxt->is_get_valid_tp_data_after_enable = true;
	}
	input_report_abs(dev, EVENT_TYPE_TP_VALUE, value);
	input_sync(dev);
        printk("tp_data_report data:%d",value);
	return 0;
}


static void tp_work_func(struct work_struct *work)
{
	struct tp_context *cxt = NULL;
	int value, status;
	int64_t  nt;
	struct timespec time;
	int err;
        printk("temperature: %s\n",__func__);
	cxt  = tp_context_obj;
	if (NULL == cxt->tp_data.get_data) {
		printk("temperature driver not register data path\n");
		return;
	}

	time.tv_sec = time.tv_nsec = 0;
	time = get_monotonic_coarse();
	nt = time.tv_sec*1000000000LL+time.tv_nsec;
	/* add wake lock to make sure data can be read before system suspend */
	err = cxt->tp_data.get_data(&value, &status);
	if (err) {
		printk("get temperature data fails!!\n");
		goto tp_loop;
	} 
	if (true ==  cxt->is_tp_first_data_after_enable) {
		cxt->is_tp_first_data_after_enable = false;
		/* filter -1 value */
		if (TEMPERATURE_INVALID_VALUE == value) {
			printk(" read invalid data\n");
			goto tp_loop;
		}
	}
	printk(" tp data[%d]\n" , value);
	tp_data_report(cxt->idev,value);

tp_loop:
	if (true == cxt->is_tp_polling_run)
		mod_timer(&cxt->timer_tp, jiffies + atomic_read(&cxt->delay_tp)/(1000/HZ));
}

static void tp_poll(unsigned long data)
{
	struct tp_context *obj = (struct tp_context *)data;
         printk("temperature: %s\n",__func__);
	if ((obj != NULL) && (obj->is_tp_polling_run))
		schedule_work(&obj->report_tp);
}

static struct tp_context *temperature_context_alloc_object(void)
{
	struct tp_context *obj = kzalloc(sizeof(*obj), GFP_KERNEL);
        printk("temperature: %s\n",__func__);
	printk("temperature_context_alloc_object++++\n");
	if (!obj) {
		printk("Alloc temperature object error!\n");
		return NULL;
	}
	atomic_set(&obj->delay_tp, 200); /*5Hz, set work queue delay time 200ms */
	atomic_set(&obj->wake, 0);
	INIT_WORK(&obj->report_tp, tp_work_func);
	init_timer(&obj->timer_tp);
	obj->timer_tp.expires	= jiffies + atomic_read(&obj->delay_tp)/(1000/HZ);
	obj->timer_tp.function	= tp_poll;
	obj->timer_tp.data	= (unsigned long)obj;
	obj->is_tp_first_data_after_enable = false;
	obj->is_tp_polling_run = false;
	mutex_init(&obj->tp_op_mutex);
	obj->is_tp_batch_enable = false;/* for batch mode init */	
	printk("temperature_context_alloc_object----\n");
	return obj;
}


static int tempera_remove(struct platform_device *pdev)
{
	printk("temperature_remove\n");
	return 0;
}

static int tempera_probe(struct platform_device *pdev)
{
	printk("temperature_probe\n");
	plt_dev = pdev;
	return 0;
}

#ifdef CONFIG_OF
static const struct of_device_id tp_of_match[] = {
	{.compatible = "mediatek,temperature",},
	{},
};
#endif



static struct platform_driver tp_driver = {
	.probe	  = tempera_probe,
	.remove	 = tempera_remove,
	.driver = {

		.name  = "temperature",
	#ifdef CONFIG_OF
		.of_match_table = tp_of_match,
		#endif
	}
};

int tp_driver_add(struct tp_init_info *obj)
{
	int err = 0;
	int i = 0;

	printk("temperature: %s\n",__func__);
	if (!obj) {
		printk("TEMPERATURE driver add fail, temp_init_info is NULL\n");
		return -1;
	}

	for (i = 0; i < MAX_CHOOSE_TEMPERATURE_NUM; i++) {
		if ((i == 0) && (NULL == tp_init_list[0])) {
			printk("register temperature driver for the first time\n");
			if (platform_driver_register(&tp_driver))
				printk("failed to register temperature driver already exist\n");
		}

		if (NULL == tp_init_list[i]) {
			obj->platform_diver_addr = &tp_driver;
			tp_init_list[i] = obj;
			break;
		}
	}
	if (i >= MAX_CHOOSE_TEMPERATURE_NUM) {
		printk("TEMPERATURE driver add err\n");
		err =  -1;
	}
	return err;
}
EXPORT_SYMBOL_GPL(tp_driver_add);

static int temperature_real_driver_init(void)
{
	int i = 0;
	int err = 0;
        printk("temperature: %s\n",__func__);
	printk(" temperature_real_driver_init +\n");
	for (i = 0; i < MAX_CHOOSE_TEMPERATURE_NUM; i++) {
		printk("temperature_real_driver_init i=%d\n", i);
		if (0 != tp_init_list[i]) {
			printk(" temperature try to init driver %s\n", tp_init_list[i]->name);
			err = tp_init_list[i]->init();
			if (0 == err) {
				printk(" temperature real driver %s probe ok\n", tp_init_list[i]->name);
				break;
			}
		}
	}

	if (i == MAX_CHOOSE_TEMPERATURE_NUM) {
		printk(" temperature_real_driver_init fail\n");
		err =  -1;
	}

	return err;
}

static int temperature_input_init(struct tp_context *cxt)
{
	struct input_dev *dev;
	int err = 0;
        printk("temperature: %s\n",__func__);
	dev = input_allocate_device();
	if (NULL == dev)
		return -ENOMEM;

	dev->name = TEMPERATURE_INPUTDEV_NAME;
	set_bit(EV_ABS, dev->evbit);
	set_bit(EV_SYN, dev->evbit);
	input_set_capability(dev, EV_ABS, EVENT_TYPE_TP_VALUE);
	input_set_abs_params(dev, EVENT_TYPE_TP_VALUE, TP_VALUE_MIN, TP_VALUE_MAX, 0, 0);
	input_set_drvdata(dev, cxt);
	err = input_register_device(dev);
	if (err < 0) {
		input_free_device(dev);
		return err;
	}
	cxt->idev = dev;
	return 0;
}


static int temperature_probe(void)
{
	int err;
        printk("temperature: %s\n",__func__);
	printk("+++++++++++++temperature_probe!!\n");
	tp_context_obj = temperature_context_alloc_object();
	if (!tp_context_obj) {
		err = -ENOMEM;
		printk("unable to allocate devobj!\n");
		goto exit_alloc_data_failed;
	}
	
	err = temperature_real_driver_init();
	if (err) {
		printk("temperature real driver init fail\n");
		goto real_driver_init_fail;
	}
	
	//err = temperature_factory_device_init();
	//if (err)
	//	printk("temperature factory device already registed\n");
	
	err = temperature_input_init(tp_context_obj);
	if (err) {
		printk("unable to register temperature input device!\n");
		goto exit_alloc_input_dev_failed;
	}
	printk("----temperature_probe OK !!\n");
	return 0;

real_driver_init_fail:
exit_alloc_input_dev_failed:
	kfree(tp_context_obj);
	tp_context_obj = NULL;
exit_alloc_data_failed:
	printk("----temperature_probe fail !!!\n");
	return err;
}

static int temperature_remove(void)
{
	int err = 0;

	printk("temperature: %s\n",__func__);
	input_unregister_device(tp_context_obj->idev);
	sysfs_remove_group(&tp_context_obj->idev->dev.kobj,
				&tp_attribute_group);

	err = misc_deregister(&tp_context_obj->mdev);
	if (err)
		printk("misc_deregister fail: %d\n", err);
	kfree(tp_context_obj);

	return 0;
}

static int __init temperature_init(void)
{
	printk("temperature: %s\n",__func__);

	if (temperature_probe()) {
		printk("failed to register temperature driver\n");
		return -ENODEV;
	}

	return 0;
}



static void __exit temperature_exit(void)
{
	temperature_remove();
	platform_driver_unregister(&tp_driver);

}
late_initcall(temperature_init);


MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("TEMPERATURE device driver");
MODULE_AUTHOR("wuchuang");
