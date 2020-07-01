#ifdef BUILD_LK
#include <debug.h>
#include "lcm_drv.h"
#include <platform/upmu_common.h>
#include <platform/upmu_hw.h>
#include <platform/mt_gpio.h>
#include <platform/mt_i2c.h> 
#include <platform/mt_pmic.h>
#include <string.h>
#include <cust_gpio_usage.h>
#else
#include <linux/string.h>
#include <linux/kernel.h>
#include "lcm_drv.h"
#include <upmu_common.h>
#include <mt_gpio.h>
#include <linux/gpio.h>
#endif

// ---------------------------------------------------------------------------
//  Local Constants
// ---------------------------------------------------------------------------

#define FRAME_WIDTH  (540)
#define FRAME_HEIGHT (960)

#define REGFLAG_DELAY             							0XFC
#define REGFLAG_END_OF_TABLE      							0xFD   // END OF REGISTERS MARKER

#define LCM_ID	

#define LCM_DSI_CMD_MODE									0

#ifndef TRUE
    #define TRUE 1
#endif

#ifndef FALSE
    #define FALSE 0
#endif

// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    								(lcm_util.set_reset_pin((v)))

#define UDELAY(n) 											(lcm_util.udelay(n))
#define MDELAY(n) 											(lcm_util.mdelay(n))


// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)										lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)					lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg											lcm_util.dsi_read_reg()
#define read_reg_v2(cmd, buffer, buffer_size)				lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)

static void lcd_power_en(unsigned char enabled)
{
	if (enabled)
	{

		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);
	}	
	else
	{
		pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	}

	MDELAY(20);

}

struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[128];
};


static struct LCM_setting_table lcm_initialization_setting[] = {
{0xB0,3,{0x98,0x85,0x0A}},
{0xC1,3,{0x00,0x00,0x00}},
{0xC2,5,{0x10,0xF7,0x80,0x08,0x0C}},
{0xC4,7,{0x70,0x19,0x23,0x00,0x0A,0x0A,0x00}},
{0xCE,5,{0x11,0x22,0x34,0x00,0x0E}},
{0xD0,6,{0x55,0x05,0x32,0xE1,0x6C,0xC0}},
{0xD2,2,{0x13,0x13}},
{0xD3,9,{0x33,0x33,0x05,0x03,0x4F,0x4F,0x11,0x17,0x22}},
{0xD5,14,{0x8B,0x00,0x00,0x00,0x01,0x64,0x01,0x64,0x01,0x64,0x00,0x00,0x00,0x00}},
{0xD6,1,{0x00}},
{0xDD,27,{0x10,0x31,0x00,0x3B,0x33,0x1D,0x00,0x3C,0x00,0x3C,0x00,0x49,0x00,0xFF,0xFF,0xF8,0xE1,0x80,0x33,0x33,0x15,0x08,0xA0,0x01,0x00,0x71,0x7F}},
{0xDE,20,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
{0xE5,71,{0x00,0x81,0x87,0x85,0x80,0x80,0x84,0x81,0x85,0x82,0x86,0x83,0x87,0x80,0x00,0x80,0x86,0x84,0x82,0x83,0x00,0x80,0x86,0x84,0x80,0x80,0x84,0x81,0x85,0x82,0x86,0x83,0x87,0x80,0x00,0x81,0x87,0x85,0x82,0x83,0x06,0x30,0x76,0x54,0x01,0xAC,0x3F,0xEF,0x01,0x01,0xAC,0x23,0x12,0x01,0xAC,0x23,0x05,0x26,0x01,0xAC,0x23,0x25,0x26,0x01,0xAC,0x00,0x00,0x00,0x00,0x00,0x00}},
{0xEA,15,{0x33,0x32,0x03,0x03,0x00,0x00,0x00,0x40,0x00,0x00,0x00,0x00,0x00,0x00,0x03}},
{0xED,23,{0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x60}},
{0xEE,19,{0x22,0x10,0x02,0x02,0x0F,0x40,0x00,0x07,0x00,0x04,0x00,0x00,0xC0,0xB9,0x77,0x00,0x55,0x05,0x1F}},
{0xEF,9,{0x3C,0x05,0x52,0x13,0xE1,0x33,0x5B,0x08,0x0C}},
{0xC7,122,{0x00,0x00,0x00,0x1A,0x00,0x4F,0x00,0x65,0x00,0x7D,0x00,0x96,0x00,0xA5,0x00,0xB5,0x00,0xC5,0x00,0xF3,0x01,0x1C,0x01,0x57,0x01,0x89,0x01,0xD5,0x02,0x13,0x02,0x15,0x02,0x4C,0x02,0x88,0x02,0xAD,0x02,0xE0,0x03,0x03,0x03,0x34,0x03,0x43,0x03,0x58,0x03,0x6C,0x03,0x7E,0x03,0xAA,0x03,0xC2,0x03,0xC7,0x03,0xE6,0x00,0x00,0x00,0x1A,0x00,0x4F,0x00,0x65,0x00,0x7D,0x00,0x96,0x00,0xA5,0x00,0xB5,0x00,0xC5,0x00,0xF3,0x01,0x1C,0x01,0x57,0x01,0x89,0x01,0xD5,0x02,0x13,0x02,0x15,0x02,0x4C,0x02,0x88,0x02,0xAD,0x02,0xE0,0x03,0x03,0x03,0x34,0x03,0x43,0x03,0x58,0x03,0x6C,0x03,0x7E,0x03,0xAA,0x03,0xC2,0x03,0xC7,0x03,0xE6,0x01,0x01}},
{0xD1,3,{0x09,0x09,0xc2}},
{0xe9,1,{0x01}},
{0xb4,2,{0x04,0x16}},

//{0x3A,1,{0x77}},
{0x11,01,{0x00}},
{REGFLAG_DELAY,120,{}},
{0x29,1,{0x00}},//Display ON 
//{0x35,1,{0x00}},
{REGFLAG_DELAY,20,{}},	


{REGFLAG_END_OF_TABLE, 0x00, {}}

};


#if 0
static struct LCM_setting_table lcm_sleep_out_setting[] = {
	// Sleep Out
	{0x11, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	// Display ON
	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
#endif

static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
	// Sleep Out
	{0x28, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},

	// Display ON
	{0x10, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};

static void push_table(struct LCM_setting_table *table, unsigned int count, unsigned char force_update)
{
    unsigned int i;

    for(i = 0; i < count; i++)
    {

        unsigned cmd;
        cmd = table[i].cmd;

        switch (cmd)
        {

            case REGFLAG_DELAY :
                MDELAY(table[i].count);
                break;

            case REGFLAG_END_OF_TABLE :
                break;

            default:
				
				dsi_set_cmdq_V2(cmd, table[i].count, table[i].para_list, force_update);
//                dsi_set_cmdq_dcs(cmd, table[i].count, table[i].para_list, force_update);
        }
    }
	
}



// ---------------------------------------------------------------------------
//  LCM Driver Implementations
// ---------------------------------------------------------------------------

static void lcm_set_util_funcs(const LCM_UTIL_FUNCS *util)
{
    memcpy(&lcm_util, util, sizeof(LCM_UTIL_FUNCS));
}


static void lcm_get_params(LCM_PARAMS *params)
{

	memset(params, 0, sizeof(LCM_PARAMS));

	params->type = LCM_TYPE_DSI;

	params->width = FRAME_WIDTH;
	params->height = FRAME_HEIGHT;

		// enable tearing-free
		params->dbi.te_mode 			= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = BURST_VDO_MODE;

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				    = LCM_TWO_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
		params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
		params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Highly depends on LCD driver capability.
		// Not support in MT6573
		params->dsi.packet_size=256;

		// Video mode setting		
		//params->dsi.intermediat_buffer_num = 2;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

		params->dsi.vertical_sync_active				= 6;//2;
		params->dsi.vertical_backporch					= 18;   // from Q driver
		params->dsi.vertical_frontporch					= 20;  // rom Q driver
		params->dsi.vertical_active_line				= FRAME_HEIGHT;

		params->dsi.horizontal_sync_active				= 20;//10;
		params->dsi.horizontal_backporch				= 80; // from Q driver
		params->dsi.horizontal_frontporch				= 80;  // from Q driver
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;


		params->dsi.PLL_CLOCK = 250;//230//220//240//450; //this value must be in MTK suggested table
		params->dsi.ssc_disable	= 1;

#if 1
		params->dsi.esd_check_enable 			= 1;
		params->dsi.customization_esd_check_enable 	= 1;
		params->dsi.lcm_esd_check_table[0].cmd 		= 0xac;//0x0A;
		params->dsi.lcm_esd_check_table[0].count 	= 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x00;//0x9C;
		params->dsi.noncont_clock = 1;
		params->dsi.noncont_clock_period = 2;
#endif
}


static void lcm_init(void)
{

	lcd_power_en(1);
    //MDELAY(100);
    SET_RESET_PIN(1);
	MDELAY(20);
    SET_RESET_PIN(0);
	MDELAY(20);
    SET_RESET_PIN(1);
	MDELAY(50);

    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);



}



static void lcm_suspend(void)
{
	SET_RESET_PIN(1);
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
	push_table(lcm_deep_sleep_mode_in_setting, sizeof(lcm_deep_sleep_mode_in_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_resume(void)
{

	lcm_init();
//	push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
}




static unsigned int lcm_compare_id(void)
{
#ifdef BUILD_LK
	unsigned int lcd_id1 = 0;

	lcd_power_en(1);
	MDELAY(50);
   	lcd_id1 = (mt_get_gpio_in(GPIO_LCD_ID1_PIN));
   	//lcd_id2 = (mt_get_gpio_in(GPIO_LCD_ID2_PIN));

	printf("lcd_id1 anan =%x\n", lcd_id1);	
	//printf("lcd_id2 anan =%x\n", lcd_id2);	
	printf("lcd_id_pin1 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));
	//printf("lcd_id_pin2 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));	

	return (lcd_id1  == 0)?1:0;
#else
	return 1;
#endif
}


LCM_DRIVER ili9885_qhd_dsi_vdo_xp_dt_dt552xxav0_mx2120e_hds_s702_lcm_drv = 
{
    .name			= "ili9885_qhd_dsi_vdo_xp_dt_dt552xxav0_mx2120e_hds_s702",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};
