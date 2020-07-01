#include <linux/types.h>
#include <mt-plat/charging.h>
#include <mt-plat/upmu_common.h>
#include <linux/delay.h>
#include <linux/reboot.h>
#include <mt-plat/mt_boot.h>
#include <mt-plat/battery_common.h>
#include <mach/mt_charging.h>
#include <mach/mt_pmic.h>
#include "fan5405.h"

/* ============================================================ // */
/* Define */
/* ============================================================ // */
#define STATUS_OK	0
#define STATUS_FAIL	1
#define STATUS_UNSUPPORTED	-1
#define GETARRAYNUM(array) (sizeof(array)/sizeof(array[0]))


/* ============================================================ // */
/* Global variable */
/* ============================================================ // */

#if defined(MTK_WIRELESS_CHARGER_SUPPORT)
#define WIRELESS_CHARGER_EXIST_STATE 0
int wireless_charger_gpio_number = (168 | 0x80000000);
#endif

const u32 VBAT_CV_VTH[] = {
	BATTERY_VOLT_03_500000_V, BATTERY_VOLT_03_520000_V, BATTERY_VOLT_03_540000_V,
	    BATTERY_VOLT_03_560000_V,
	BATTERY_VOLT_03_580000_V, BATTERY_VOLT_03_600000_V, BATTERY_VOLT_03_620000_V,
	    BATTERY_VOLT_03_640000_V,
	BATTERY_VOLT_03_660000_V, BATTERY_VOLT_03_680000_V, BATTERY_VOLT_03_700000_V,
	    BATTERY_VOLT_03_720000_V,
	BATTERY_VOLT_03_740000_V, BATTERY_VOLT_03_760000_V, BATTERY_VOLT_03_780000_V,
	    BATTERY_VOLT_03_800000_V,
	BATTERY_VOLT_03_820000_V, BATTERY_VOLT_03_840000_V, BATTERY_VOLT_03_860000_V,
	    BATTERY_VOLT_03_880000_V,
	BATTERY_VOLT_03_900000_V, BATTERY_VOLT_03_920000_V, BATTERY_VOLT_03_940000_V,
	    BATTERY_VOLT_03_960000_V,
	BATTERY_VOLT_03_980000_V, BATTERY_VOLT_04_000000_V, BATTERY_VOLT_04_020000_V,
	    BATTERY_VOLT_04_040000_V,
	BATTERY_VOLT_04_060000_V, BATTERY_VOLT_04_080000_V, BATTERY_VOLT_04_100000_V,
	    BATTERY_VOLT_04_120000_V,
	BATTERY_VOLT_04_140000_V, BATTERY_VOLT_04_160000_V, BATTERY_VOLT_04_180000_V,
	    BATTERY_VOLT_04_200000_V,
	BATTERY_VOLT_04_220000_V, BATTERY_VOLT_04_240000_V, BATTERY_VOLT_04_260000_V,
	    BATTERY_VOLT_04_280000_V,
	BATTERY_VOLT_04_300000_V, BATTERY_VOLT_04_320000_V, BATTERY_VOLT_04_340000_V,
	    BATTERY_VOLT_04_360000_V,
	BATTERY_VOLT_04_380000_V, BATTERY_VOLT_04_400000_V, BATTERY_VOLT_04_420000_V,
	    BATTERY_VOLT_04_440000_V
};
#ifdef CONFIG_BIRD_PSC5420_CHARGE_IC_SUPPORT
	#if defined (CONFIG_BIRD_PSC5420_CHARGE_IC_SUPPORT_1_5A)//33m
	const u32 CS_VTH[] = {
		CHARGE_CURRENT_750_00_MA/*DATASHEET 720*/, CHARGE_CURRENT_1150_00_MA/*DATASHEET 1333*/,
		CHARGE_CURRENT_1600_00_MA/*DATASHEET 1636*/
	};
	#elif defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_1_9A)//33m
	const u32 CS_VTH[] = {
		CHARGE_CURRENT_600_00_MA/*DATASHEET 600*/, CHARGE_CURRENT_1000_00_MA/*DATASHEET 1000*/,
		CHARGE_CURRENT_1300_00_MA/*DATASHEET 1300*/, CHARGE_CURRENT_1600_00_MA/*DATASHEET 1600*/,
		CHARGE_CURRENT_1900_00_MA/*DATASHEET 1900*/
	};
	#elif defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_2_2A)//33m
	const u32 CS_VTH[] = {
		CHARGE_CURRENT_600_00_MA/*DATASHEET 600*/, CHARGE_CURRENT_1000_00_MA/*DATASHEET 1000*/,
		CHARGE_CURRENT_1300_00_MA/*DATASHEET 1300*/, CHARGE_CURRENT_1600_00_MA/*DATASHEET 1600*/,
		CHARGE_CURRENT_1900_00_MA/*DATASHEET 1900*/, CHARGE_CURRENT_2200_00_MA/*DATASHEET 2200*/
	};
	#elif defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_2_4A)//33m
	const u32 CS_VTH[] = {
		CHARGE_CURRENT_600_00_MA/*DATASHEET 600*/, CHARGE_CURRENT_1000_00_MA/*DATASHEET 1000*/,
		CHARGE_CURRENT_1300_00_MA/*DATASHEET 1300*/, CHARGE_CURRENT_1600_00_MA/*DATASHEET 1600*/,
		CHARGE_CURRENT_1900_00_MA/*DATASHEET 1900*/, CHARGE_CURRENT_2200_00_MA/*DATASHEET 2200*/,
		CHARGE_CURRENT_2400_00_MA/*DATASHEET 2400*/
	};

	#elif defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_2_XA)//33m
	const u32 CS_VTH[] = {
		CHARGE_CURRENT_600_00_MA/*DATASHEET 600*/, CHARGE_CURRENT_1000_00_MA/*DATASHEET 1000*/,
		CHARGE_CURRENT_1300_00_MA/*DATASHEET 1300*/, CHARGE_CURRENT_1600_00_MA/*DATASHEET 1600*/,
		CHARGE_CURRENT_1900_00_MA/*DATASHEET 1900*/, CHARGE_CURRENT_2200_00_MA/*DATASHEET 2200*/,
		CHARGE_CURRENT_2300_00_MA/*DATASHEET 2400*/, CHARGE_CURRENT_2400_00_MA/*DATASHEET 2X00*/,
	};

	#else //33m
	const u32 CS_VTH[] = {
		CHARGE_CURRENT_750_00_MA/*DATASHEET 720*/, CHARGE_CURRENT_1150_00_MA/*DATASHEET 1333*/,
		CHARGE_CURRENT_1600_00_MA/*DATASHEET 1636*/, CHARGE_CURRENT_2000_00_MA/*DATASHEET 1969*/,
	};
	#endif
#else
const u32 CS_VTH[] = {
	CHARGE_CURRENT_550_00_MA, CHARGE_CURRENT_650_00_MA, CHARGE_CURRENT_750_00_MA,
	    CHARGE_CURRENT_850_00_MA,
	CHARGE_CURRENT_950_00_MA, CHARGE_CURRENT_1050_00_MA, CHARGE_CURRENT_1150_00_MA,
	    CHARGE_CURRENT_1250_00_MA
};
#endif

const u32 INPUT_CS_VTH[] = {
	CHARGE_CURRENT_100_00_MA, CHARGE_CURRENT_500_00_MA, CHARGE_CURRENT_800_00_MA,
	    CHARGE_CURRENT_MAX
};

const u32 VCDT_HV_VTH[] = {
	BATTERY_VOLT_04_200000_V, BATTERY_VOLT_04_250000_V, BATTERY_VOLT_04_300000_V,
	    BATTERY_VOLT_04_350000_V,
	BATTERY_VOLT_04_400000_V, BATTERY_VOLT_04_450000_V, BATTERY_VOLT_04_500000_V,
	    BATTERY_VOLT_04_550000_V,
	BATTERY_VOLT_04_600000_V, BATTERY_VOLT_06_000000_V, BATTERY_VOLT_06_500000_V,
	    BATTERY_VOLT_07_000000_V,
	BATTERY_VOLT_07_500000_V, BATTERY_VOLT_08_500000_V, BATTERY_VOLT_09_500000_V,
	    BATTERY_VOLT_10_500000_V
};

u32 charging_value_to_parameter(const u32 *parameter, const u32 array_size, const u32 val)
{
	if (val < array_size)
		return parameter[val];
	battery_log(BAT_LOG_CRTI, "Can't find the parameter \r\n");
	return parameter[0];
}

u32 charging_parameter_to_value(const u32 *parameter, const u32 array_size, const u32 val)
{
	u32 i;

	for (i = 0; i < array_size; i++)
		if (val == *(parameter + i))
			return i;

	battery_log(BAT_LOG_CRTI, "NO register value match \r\n");

	return 0;
}


static u32 bmt_find_closest_level(const u32 *pList, u32 number, u32 level)
{
	u32 i;
	u32 max_value_in_last_element;

	if (pList[0] < pList[1])
		max_value_in_last_element = KAL_TRUE;
	else
		max_value_in_last_element = KAL_FALSE;

	if (max_value_in_last_element == KAL_TRUE) {
		for (i = (number - 1); i != 0; i--)	/* max value in the last element */
			if (pList[i] <= level)
				return pList[i];

		battery_log(BAT_LOG_CRTI, "Can't find closest level, small value first \r\n");
		return pList[0];
		/* return CHARGE_CURRENT_0_00_MA; */
	} else {
		for (i = 0; i < number; i++)	/* max value in the first element */
			if (pList[i] <= level)
				return pList[i];

		battery_log(BAT_LOG_CRTI, "Can't find closest level, large value first \r\n");
		return pList[number - 1];
		/* return CHARGE_CURRENT_0_00_MA; */
	}
}

static u32 charging_hw_init(void *data)
{
	u32 status = STATUS_OK;
	static bool charging_init_flag = KAL_FALSE;

#if defined(CONFIG_BIRD_PSC5420_CHARGE_IC_SUPPORT)
///zhou
  #if defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_1_9A)||defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_2_2A)||defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_2_2A)||defined (CONFIG_BIRD_PSC5425_CHARGE_IC_SUPPORT_2_XA)
	fan5405_reg_config_interface(0x06, 0x79); //59 69 79 when current level ,modify this one
  #else
	fan5405_reg_config_interface(0x06, 0x49); //59 69 79 when current level ,modify this one
  #endif
	fan5405_reg_config_interface(0x01, 0xf8); //f8
	fan5405_reg_config_interface(0x02, 0x94); 
	fan5405_reg_config_interface(0x04, 0x30);
	fan5405_reg_config_interface(0x05, 0x04);  //0x04 or 0x84 20170331

	if (!charging_init_flag) {
	fan5405_reg_config_interface(0x04, 0x30);
	charging_init_flag = KAL_TRUE;
	}


#elif defined(CONFIG_BIRD_HL7007_CHARGE_IC_SUPPORT_2450MA)
///zhou
  #if defined (CONFIG_HIGH_BATTERY_VOLTAGE_SUPPORT)
	fan5405_reg_config_interface(0x06, 0x79); //59 69 79 when current level ,modify this one
  #else
	fan5405_reg_config_interface(0x06, 0x70); //59 69 79 when current level ,modify this one
  #endif
	fan5405_reg_config_interface(0x01, 0xf8); //f8
	fan5405_reg_config_interface(0x02, 0xAC);
	//fan5405_reg_config_interface(0x04, 0x38);
	fan5405_reg_config_interface(0x05, 0x03);  //0x04 or 0x84 20170331

	if (!charging_init_flag) {
	//fan5405_reg_config_interface(0x04, 0x38);
	charging_init_flag = KAL_TRUE;
	}

#else

#if defined(MTK_WIRELESS_CHARGER_SUPPORT)
	mt_set_gpio_mode(wireless_charger_gpio_number, 0);	/* 0:GPIO mode */
	mt_set_gpio_dir(wireless_charger_gpio_number, 0);	/* 0: input, 1: output */
#endif

#if defined(CONFIG_HIGH_BATTERY_VOLTAGE_SUPPORT)
	fan5405_reg_config_interface(0x06, 0x77); // ISAFE = 1250mA, VSAFE = 4.34V
#else
	fan5405_reg_config_interface(0x06, 0x70);
#endif

	fan5405_reg_config_interface(0x00, 0xC0);	/* kick chip watch dog */
	fan5405_reg_config_interface(0x01, 0xb8);	/* TE=1, CE=0, HZ_MODE=0, OPA_MODE=0 */
	fan5405_reg_config_interface(0x05, 0x03);
	if (!charging_init_flag) {
		fan5405_reg_config_interface(0x04, 0x1A);	/* 146mA */
		charging_init_flag = KAL_TRUE;
	}

#endif


	return status;
}


static u32 charging_dump_register(void *data)
{
	u32 status = STATUS_OK;

	fan5405_dump_register();

	return status;
}


static u32 charging_enable(void *data)
{
	u32 status = STATUS_OK;
	u32 enable = *(u32 *) (data);

	if (KAL_TRUE == enable) {
		fan5405_set_ce(0);
		fan5405_set_hz_mode(0);
		fan5405_set_opa_mode(0);
	} else {

#if defined(CONFIG_USB_MTK_HDRC_HCD)
		if (mt_usb_is_device())
#endif
#ifdef CONFIG_BIRD_PSC5420_CHARGE_IC_SUPPORT
		fan5405_set_ce(1);
		fan5405_set_hz_mode(1);
		fan5405_set_opa_mode(1);
#else
		fan5405_set_ce(1);
#endif
	}

	return status;
}


static u32 charging_set_cv_voltage(void *data)
{
	u32 status = STATUS_OK;
	u16 register_value;

	register_value =
	    charging_parameter_to_value(VBAT_CV_VTH, GETARRAYNUM(VBAT_CV_VTH), *(u32 *) (data));
	fan5405_set_oreg(register_value);

	return status;
}


static u32 charging_get_current(void *data)
{
	u32 status = STATUS_OK;
	u32 array_size;
	u8 reg_value;

	/* Get current level */
	array_size = GETARRAYNUM(CS_VTH);
	fan5405_read_interface(0x1, &reg_value, 0x3, 0x6);	/* IINLIM */
	*(u32 *) data = charging_value_to_parameter(CS_VTH, array_size, reg_value);

	return status;
}



static u32 charging_set_current(void *data)
{
	u32 status = STATUS_OK;
	u32 set_chr_current;
	u32 array_size;
	u32 register_value;
	u32 current_value = *(u32 *) data;

	if (current_value <= CHARGE_CURRENT_350_00_MA) {
	#ifdef CONFIG_BIRD_PSC5420_CHARGE_IC_SUPPORT
		//fan5405_set_io_level(3);
	#else
		fan5405_set_io_level(1);
	#endif
	} else {
	#ifdef CONFIG_BIRD_PSC5420_CHARGE_IC_SUPPORT
		//fan5405_set_io_level(3);
	#else
		fan5405_set_io_level(0);
	#endif
		array_size = GETARRAYNUM(CS_VTH);
		set_chr_current = bmt_find_closest_level(CS_VTH, array_size, current_value);
		register_value = charging_parameter_to_value(CS_VTH, array_size, set_chr_current);

	#if defined (CONFIG_BIRD_HL7007_CHARGE_IC_SUPPORT_2450MA)
		fan5405_reg_config_interface(0x04, 0x78);
	#else
		fan5405_set_iocharge(register_value);
	#endif
	}
	return status;
}


static u32 charging_set_input_current(void *data)
{
	u32 status = STATUS_OK;
	u32 set_chr_current;
	u32 array_size;
	u32 register_value;

	if (*(u32 *) data > CHARGE_CURRENT_500_00_MA) {
		register_value = 0x3;
	} else {
		array_size = GETARRAYNUM(INPUT_CS_VTH);
		set_chr_current = bmt_find_closest_level(INPUT_CS_VTH, array_size, *(u32 *) data);
		register_value =
		    charging_parameter_to_value(INPUT_CS_VTH, array_size, set_chr_current);
	}

	fan5405_set_input_charging_current(register_value);

	return status;
}


static u32 charging_get_charging_status(void *data)
{
	u32 status = STATUS_OK;
	u32 ret_val;

	ret_val = fan5405_get_chip_status();

	if (ret_val == 0x2)
		*(u32 *) data = KAL_TRUE;
	else
		*(u32 *) data = KAL_FALSE;

	return status;
}


static u32 charging_reset_watch_dog_timer(void *data)
{
	u32 status = STATUS_OK;

	fan5405_set_tmr_rst(1);

	return status;
}


static u32 charging_set_hv_threshold(void *data)
{
	u32 status = STATUS_OK;

	u32 set_hv_voltage;
	u32 array_size;
	u16 register_value;
	u32 voltage = *(u32 *) (data);

	array_size = GETARRAYNUM(VCDT_HV_VTH);
	set_hv_voltage = bmt_find_closest_level(VCDT_HV_VTH, array_size, voltage);
	register_value = charging_parameter_to_value(VCDT_HV_VTH, array_size, set_hv_voltage);
	pmic_set_register_value(PMIC_RG_VCDT_HV_VTH, register_value);
	return status;
}


static u32 charging_get_hv_status(void *data)
{
	u32 status = STATUS_OK;

#if defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)
	*(kal_bool *) (data) = 0;
#else
	*(kal_bool *) (data) = pmic_get_register_value(PMIC_RGS_VCDT_HV_DET);
#endif
	return status;
}


static u32 charging_get_battery_status(void *data)
{
	unsigned int status = STATUS_OK;
   
#if 0 //defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)
	*(kal_bool *) (data) = 0;	/* battery exist */
	battery_log(BAT_LOG_CRTI, "[charging_get_battery_status] battery exist for bring up.\n");
#else
	unsigned int val = 0;

	val = pmic_get_register_value(PMIC_BATON_TDET_EN);
	battery_log(BAT_LOG_FULL, "[charging_get_battery_status] BATON_TDET_EN = %d\n", val);
	if (val) {
		pmic_set_register_value(PMIC_BATON_TDET_EN, 1);
		pmic_set_register_value(PMIC_RG_BATON_EN, 1);
		*(kal_bool *) (data) = pmic_get_register_value(PMIC_RGS_BATON_UNDET);
	} else {
		*(kal_bool *) (data) = KAL_FALSE;
	}
#endif

	return status;
}


static u32 charging_get_charger_det_status(void *data)
{
	unsigned int status = STATUS_OK;
	unsigned int val = 0;

#if defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)
	val = 1;
	battery_log(BAT_LOG_CRTI, "[charging_get_charger_det_status] chr exist for fpga.\n");
#else
	val = pmic_get_register_value(PMIC_RGS_CHRDET);
#endif

	*(kal_bool *) (data) = val;

	return status;
}

static u32 charging_get_charger_type(void *data)
{
	u32 status = STATUS_OK;

#if defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)
	*(CHARGER_TYPE *) (data) = STANDARD_HOST;
#else
	*(CHARGER_TYPE *) (data) = hw_charging_get_charger_type();
#endif

	return status;
}



static u32 charging_set_platform_reset(void *data)
{
	unsigned int status = STATUS_OK;

#if defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)
#else
	battery_log(BAT_LOG_CRTI, "charging_set_platform_reset\n");

	kernel_restart("battery service reboot system");
	/* arch_reset(0,NULL); */
#endif

	return status;
}

static u32 charging_get_platform_boot_mode(void *data)
{
	unsigned int status = STATUS_OK;

#if defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)
#else
	*(unsigned int *) (data) = get_boot_mode();

	battery_log(BAT_LOG_CRTI, "get_boot_mode=%d\n", get_boot_mode());
#endif

	return status;
}

static u32 charging_set_power_off(void *data)
{
	unsigned int status = STATUS_OK;

#if defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)
#else
	battery_log(BAT_LOG_CRTI, "charging_set_power_off\n");
	kernel_power_off();
#endif

	return status;
}

static u32 charging_set_ta_current_pattern(void *data)
{
	return STATUS_UNSUPPORTED;
}




static unsigned int charging_set_vbus_ovp_en(void *data)
{
	return STATUS_OK;
}

static unsigned int charging_set_vindpm(void *data)
{
	return STATUS_OK;
}

static unsigned int(*charging_func[CHARGING_CMD_NUMBER]) (void *data);

/*
* FUNCTION
*		Internal_chr_control_handler
*
* DESCRIPTION
*		 This function is called to set the charger hw
*
* CALLS
*
* PARAMETERS
*		None
*
* RETURNS
*
*
* GLOBALS AFFECTED
*	   None
*/

signed int chr_control_interface(CHARGING_CTRL_CMD cmd, void *data)
{
	static signed int init = -1;

	if (init == -1) {
		init = 0;
		charging_func[CHARGING_CMD_INIT] = charging_hw_init;
		charging_func[CHARGING_CMD_DUMP_REGISTER] = charging_dump_register;
		charging_func[CHARGING_CMD_ENABLE] = charging_enable;
		charging_func[CHARGING_CMD_SET_CV_VOLTAGE] = charging_set_cv_voltage;
		charging_func[CHARGING_CMD_GET_CURRENT] = charging_get_current;
		charging_func[CHARGING_CMD_SET_CURRENT] = charging_set_current;
		charging_func[CHARGING_CMD_SET_INPUT_CURRENT] = charging_set_input_current;
		charging_func[CHARGING_CMD_GET_CHARGING_STATUS] =  charging_get_charging_status;
		charging_func[CHARGING_CMD_RESET_WATCH_DOG_TIMER] = charging_reset_watch_dog_timer;
		charging_func[CHARGING_CMD_SET_HV_THRESHOLD] = charging_set_hv_threshold;
		charging_func[CHARGING_CMD_GET_HV_STATUS] = charging_get_hv_status;
		charging_func[CHARGING_CMD_GET_BATTERY_STATUS] = charging_get_battery_status;
		charging_func[CHARGING_CMD_GET_CHARGER_DET_STATUS] = charging_get_charger_det_status;
		charging_func[CHARGING_CMD_GET_CHARGER_TYPE] = charging_get_charger_type;
		charging_func[CHARGING_CMD_SET_PLATFORM_RESET] = charging_set_platform_reset;
		charging_func[CHARGING_CMD_GET_PLATFORM_BOOT_MODE] = charging_get_platform_boot_mode;
		charging_func[CHARGING_CMD_SET_POWER_OFF] = charging_set_power_off;
		charging_func[CHARGING_CMD_SET_TA_CURRENT_PATTERN] = charging_set_ta_current_pattern;
		charging_func[CHARGING_CMD_SET_VBUS_OVP_EN] = charging_set_vbus_ovp_en;
		charging_func[CHARGING_CMD_SET_VINDPM] = charging_set_vindpm;
	}

	if (cmd < CHARGING_CMD_NUMBER) {
		if (charging_func[cmd] != NULL)
			return charging_func[cmd](data);
	}

	pr_debug("[%s]UNSUPPORT Function: %d\n", __func__, cmd);

	return STATUS_UNSUPPORTED;
}

