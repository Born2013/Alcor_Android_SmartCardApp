#include <linux/ioctl.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/input.h>
#include <linux/workqueue.h>
#include <linux/timer.h>
#include <linux/interrupt.h>
#include <linux/fs.h>
#include <linux/miscdevice.h>
#include <linux/platform_device.h>
#include <linux/gpio.h>
#include <mt-plat/aee.h>
#include <linux/atomic.h>
#include <linux/kernel.h>
#include <linux/delay.h>
//#include <mt-plat/mtk_boot_common.h>

#include <linux/wakelock.h>
#include <linux/of.h>
#include <linux/of_address.h>
#include <linux/of_irq.h>
#include <linux/clk.h>
#include <linux/debugfs.h>

#define EXTKEY_NAME	"EXTKEY"
#define EXTKEY_SAY		"extkey: "

#ifdef CONFIG_MTK_ENG_BUILD
#define EXTKEY_DEBUG 1
#else
#define EXTKEY_DEBUG 0
#endif

#if EXTKEY_DEBUG
#define extkey_print(fmt, arg...)	do { \
		pr_info(EXTKEY_SAY fmt, ##arg); \
	} while (0)
#define extkey_info(fmt, arg...)	do { \
		pr_info(EXTKEY_SAY fmt, ##arg); \
	} while (0)
#else
#define extkey_print(fmt, arg...)	do {} while (0)
#define extkey_info(fmt, arg...)	do {} while (0)
#endif

#define KEYNUM  2

struct extkey_pin_desc {
	unsigned int KeyGpio;
	unsigned int KeyValue;
};

struct extkey_pin_desc pins_desc[KEYNUM]={
	{9,   KEY_F5},	// CAM
	{21,   KEY_F7},	// PTT
};

//static unsigned int extkey_irqnr;
struct input_dev *extkey_input_dev;
static bool extkey_suspend;
struct wake_lock extkey_suspend_lock;	/* For suspend usage */

static int extkey_pdrv_probe(struct platform_device *pdev);
static int extkey_pdrv_remove(struct platform_device *pdev);

#ifndef USE_EARLY_SUSPEND
static int extkey_pdrv_suspend(struct platform_device *pdev, pm_message_t state);
static int extkey_pdrv_resume(struct platform_device *pdev);
#endif

static const struct of_device_id extkey_of_match[] = {
	{.compatible = "mediatek,extkey"},
	{},
};

static struct platform_driver extkey_pdrv = {
	.probe = extkey_pdrv_probe,
	.remove = extkey_pdrv_remove,
#ifndef USE_EARLY_SUSPEND
	.suspend = extkey_pdrv_suspend,
	.resume = extkey_pdrv_resume,
#endif
	.driver = {
		   .name = EXTKEY_NAME,
		   .owner = THIS_MODULE,
		   .of_match_table = extkey_of_match,
		   },
};

static int extkey1_eint_state;
static int extkey1_eint_irq;
static irqreturn_t extkey1_eint_handler(int irq, void *data)
{
	/* bool pressed; */

	if (extkey1_eint_state == 0) {
		irq_set_irq_type(extkey1_eint_irq, IRQ_TYPE_LEVEL_HIGH);
		extkey1_eint_state = 1;
	} else {
		irq_set_irq_type(extkey1_eint_irq, IRQ_TYPE_LEVEL_LOW);
		extkey1_eint_state = 0;
	}

	input_report_key(extkey_input_dev, pins_desc[0].KeyValue, extkey1_eint_state);
	input_sync(extkey_input_dev);

	return IRQ_HANDLED;
}

/***********************************************************************/
void extkey1_eint_setup_register(void)
{
	int ints[2] = {0, 0};
	int ret;
	struct device_node *node;

	/* register EINT handler for MRDUMP_EXT_RST key */
	node = of_find_compatible_node(NULL, NULL, "mediatek,extkey1_eint");
	if (!node)
		extkey_print("can't find compatible node\n");
	else {
		of_property_read_u32_array(node, "debounce", ints, ARRAY_SIZE(ints));
		gpio_set_debounce(ints[0], ints[1]);

		extkey1_eint_irq = irq_of_parse_and_map(node, 0);
		ret = request_irq(extkey1_eint_irq, extkey1_eint_handler,IRQF_TRIGGER_NONE, "extkey1_eint-eint", NULL);
		if (ret > 0)
			extkey_print("EINT IRQ LINE NOT AVAILABLE\n");
	}
}

static int extkey2_eint_state;
static int extkey2_eint_irq;
static irqreturn_t extkey2_eint_handler(int irq, void *data)
{
	/* bool pressed; */

	if (extkey2_eint_state == 0) {
		irq_set_irq_type(extkey2_eint_irq, IRQ_TYPE_LEVEL_HIGH);
		extkey2_eint_state = 1;
	} else {
		irq_set_irq_type(extkey2_eint_irq, IRQ_TYPE_LEVEL_LOW);
		extkey2_eint_state = 0;
	}

	input_report_key(extkey_input_dev, pins_desc[1].KeyValue, extkey2_eint_state);
	input_sync(extkey_input_dev);

	return IRQ_HANDLED;
}

/***********************************************************************/
void extkey2_eint_setup_register(void)
{
	int ints[2] = {0, 0};
	int ret;
	struct device_node *node;

	/* register EINT handler for MRDUMP_EXT_RST key */
	node = of_find_compatible_node(NULL, NULL, "mediatek,extkey2_eint");
	if (!node)
		extkey_print("can't find compatible node\n");
	else {
		of_property_read_u32_array(node, "debounce", ints, ARRAY_SIZE(ints));
		gpio_set_debounce(ints[0], ints[1]);

		extkey2_eint_irq = irq_of_parse_and_map(node, 0);
		ret = request_irq(extkey2_eint_irq, extkey2_eint_handler,IRQF_TRIGGER_NONE, "extkey2_eint-eint", NULL);
		if (ret > 0)
			extkey_print("EINT IRQ LINE NOT AVAILABLE\n");
	}
}

static int extkey_open(struct input_dev *dev)
{
	return 0;
}

static int extkey_pdrv_probe(struct platform_device *pdev)
{
	int i, r;

	extkey_info("extkey probe start!!!\n");

	extkey_input_dev = input_allocate_device();
	if (!extkey_input_dev) {
		extkey_print("input allocate device fail.\n");
		return -ENOMEM;
	}

	extkey_input_dev->name = EXTKEY_NAME;
	extkey_input_dev->id.bustype = BUS_HOST;
	extkey_input_dev->id.vendor = 0x2018;
	extkey_input_dev->id.product = 0x0119;
	extkey_input_dev->id.version = 0x0001;
	extkey_input_dev->open = extkey_open;


	__set_bit(EV_KEY, extkey_input_dev->evbit);

	for (i=0; i<KEYNUM; i++){
		__set_bit(pins_desc[i].KeyValue, extkey_input_dev->keybit);
	}

	__set_bit(EV_SW, extkey_input_dev->evbit);
	__set_bit(SW_LID, extkey_input_dev->swbit);

	extkey_input_dev->dev.parent = &pdev->dev;
	r = input_register_device(extkey_input_dev);
	if (r) {
		extkey_info("register input device failed (%d)\n", r);
		input_free_device(extkey_input_dev);
		return r;
	}

	extkey1_eint_setup_register();
	extkey2_eint_setup_register();

	extkey_info("%s Done\n", __func__);

	return 0;
}

/* should never be called */
static int extkey_pdrv_remove(struct platform_device *pdev)
{
	return 0;
}

#ifndef USE_EARLY_SUSPEND
static int extkey_pdrv_suspend(struct platform_device *pdev, pm_message_t state)
{
	extkey_suspend = true;
	extkey_print("suspend!! (%d)\n", extkey_suspend);
	return 0;
}

static int extkey_pdrv_resume(struct platform_device *pdev)
{
	extkey_suspend = false;
	extkey_print("resume!! (%d)\n", extkey_suspend);
	return 0;
}
#else
#define extkey_pdrv_suspend	NULL
#define extkey_pdrv_resume	NULL
#endif

static int __init extkey_mod_init(void)
{
	int r;

	r = platform_driver_register(&extkey_pdrv);
	if (r) {
		extkey_info("register driver failed (%d)\n", r);
		return r;
	}

	return 0;
}

/* should never be called */
static void __exit extkey_mod_exit(void)
{
}

module_init(extkey_mod_init);
module_exit(extkey_mod_exit);

MODULE_AUTHOR("Crystal Shen <crystal.shen@163.com.com>");
MODULE_DESCRIPTION("BIRD ExtKey Driver V0.0");
MODULE_LICENSE("GPL");
