
#ifndef __TEMPERATURE_H__
#define __TEMPERATURE_H__

#include <linux/i2c.h>
#include <linux/irq.h>
#include <linux/uaccess.h>
#include <linux/kobject.h>
#include <linux/types.h>
#include <linux/atomic.h>
#include <linux/io.h>
#include <linux/sched.h>
#include <linux/wakelock.h>
#include <linux/interrupt.h>
#include <linux/miscdevice.h>
#include <linux/platform_device.h>
#include <linux/input.h>
#include <linux/workqueue.h>
#include <linux/slab.h>
#include <linux/delay.h>
#include <linux/module.h>

#include <batch.h>
#include <sensors_io.h>
#include <hwmsensor.h>
#include <hwmsen_dev.h>

#define EVENT_TYPE_ALS_VALUE			ABS_X
#define TEMPERATURE_INVALID_VALUE -1

#define EVENT_TYPE_TP_VALUE			ABS_THROTTLE



#define TP_VALUE_MAX (100)
#define TP_VALUE_MIN (-40)



#define MAX_CHOOSE_TEMPERATURE_NUM 5

struct tp_control_path {
	int (*open_report_data)(int open);/* open data rerport to HAL */
	int (*enable_nodata)(int en);/* only enable not report event to HAL */
	int (*set_delay)(u64 delay);
	int (*access_data_fifo)(void);/* version2.used for flush operate */
	bool is_report_input_direct;
	bool is_support_batch;/* version2.used for batch mode support flag */
	bool is_polling_mode;
	bool is_use_common_factory;
};



struct tp_data_path {
	int (*get_data)(int *tp_value, int *status);
	int (*tp_get_raw_data)(int *tp_value);
	int vender_div;
};

struct tp_init_info {
	char *name;
	int (*init)(void);
	int (*uninit)(void);
	struct platform_driver *platform_diver_addr;
};

struct tp_context {
	struct input_dev		*idev;
	struct miscdevice	mdev;
	struct work_struct	report_tp;
	struct mutex			tp_op_mutex;
	struct timer_list		timer_tp;  /*als polling timer */
	

	atomic_t			trace;
	atomic_t			delay_tp; /*als polling period for reporting input event*/
	atomic_t			wake;  /*user-space request to wake-up, used with stop*/

	atomic_t			early_suspend;

	struct tp_control_path  tp_ctl;
	struct tp_data_path	tp_data;

        bool is_tp_active_nodata;
        bool is_tp_active_data;
        bool is_tp_first_data_after_enable;
        bool is_tp_polling_run;
        bool is_tp_batch_enable;
        bool is_get_valid_tp_data_after_enable;

};

extern int tp_driver_add(struct tp_init_info *obj);
extern int tp_register_control_path(struct tp_control_path *ctl);
extern int tp_register_data_path(struct tp_data_path *data);
extern int tp_data_report(struct input_dev *dev, int value);
#endif
