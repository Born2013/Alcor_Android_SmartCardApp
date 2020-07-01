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

#define FRAME_WIDTH  										(480)
#define FRAME_HEIGHT 										(854)

#define REGFLAG_DELAY             							0XFC
#define REGFLAG_END_OF_TABLE      							0xFD   // END OF REGISTERS MARKER

#define LCM_ID                                                                                  (0x9806) 

#define LCM_DSI_CMD_MODE									0

#ifndef TRUE
    #define   TRUE     1
#endif
 
#ifndef FALSE
    #define   FALSE    0
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

		//pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		//pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);
	}	
	else
	{
		//pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	}
	MDELAY(20);

}


struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[64];
};


static struct LCM_setting_table lcm_initialization_setting[] = {
{0xBF,3,{0x91,0x61,0xF2}},


{0xB3,2,{0x00,0x3d}},//34


{0xB4,2,{0x00,0x34}},


{0xB8,6,{0x00,0xBF,0x01,0x00,0xBF,0x01}},


{0xBA,3,{0x3E,0x23,0x00}},


{0xC3,1,{0x04}},


{0xC4,2,{0x30,0x6A}},


{0xC7,9,{0x00,0x01,0x31,0x0A,0x6A,0x2C,0x0F,0xA5,0xA5}},


{0xC8,38,{0x7F,0x60,0x4b,0x3b,0x33,0x22,0x25,0x0f,0x29,0x29,0x2b,0x4a,0x36,0x3A,0x28,0x21,0x14,
          0x07, 0x00,0x7F,0x60,0x4b,0x3b,0x33,0x22,0x25,0x0f,0x29,0x29,0x2b,0x4a,0x36,0x3A,0x28,
          0x21,0x14,0x07,0x00}},

{0xD4,16,{0x1F,0x1F,0x1F,0x03,0x01,0x05,0x07,0x09,0x0B,0x11,0x13,0x1F,0x1F,0x1F,0x1F,0x1F}},


{0xD5,16,{0x1F,0x1F,0x1F,0x02,0x00,0x04,0x06,0x08,0x0A,0x10,0x12,0x1F,0x1F,0x1F,0x1F,0x1F}},


{0xD6,16,{0x1F,0x1F,0x1F,0x10,0x12,0x04,0x0A,0x08,0x06,0x02,0x00,0x1F,0x1F,0x1F,0x1F,0x1F}},


{0xD7,16,{0x1F,0x1F,0x1F,0x11,0x13,0x05,0x0B,0x09,0x07,0x03,0x01,0x1F,0x1F,0x1F,0x1F,0x1F}},


{0xD8,20,{0x20,0x00,0x00,0x30,0x03,0x30,0x01,0x02,0x30,0x01,0x02,0x06,0x70,0x73,0x5D,0x72,0x06,0x38,0x70,0x08}},


{0xD9,19,{0x00,0x0A,0x0A,0x88,0x00,0x00,0x06,0x7B,0x00,0x80,0x00,0x3B,0x33,0x1F,0x00,0x00,0x00,0x06,0x70}},


{0xBE,1,{0x01}},
{0xC1,1,{0x10}}, // TP strengthn 2016-01-19 KYD GAN

{0xCC,10,{0x34,0x20,0x38,0x60,0x11,0x91,0x00,0x40,0x00,0x00}},

{0xBE,1,{0x00}},


{0x11,1,{0x00}},
//{0x21, 0,{0x00}},
{REGFLAG_DELAY, 100, {}},


//{0x21, 0,{0x00}},
{0x29, 0,{0x00}},
{REGFLAG_DELAY, 50, {}},


{REGFLAG_END_OF_TABLE,0x00,{}},
};


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

    for(i = 0; i < count; i++) {
		
        unsigned cmd;
        cmd = table[i].cmd;
		
        switch (cmd) {
			
            case REGFLAG_DELAY :
                MDELAY(table[i].count);
                break;
				
            case REGFLAG_END_OF_TABLE :
                break;
				
            default:
				dsi_set_cmdq_V2(cmd, table[i].count, table[i].para_list, force_update);
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
	
		params->type   = LCM_TYPE_DSI;

		params->width  = FRAME_WIDTH;
		params->height = FRAME_HEIGHT;

		// enable tearing-free
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode  = SYNC_PULSE_VDO_MODE; //BURST_VDO_MODE;//

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				= LCM_TWO_LANE;
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

	params->dsi.vertical_sync_active				= 4;//1010
	params->dsi.vertical_backporch				= 6;//10 30
	params->dsi.vertical_frontporch				= 6;//30 30 
	params->dsi.vertical_active_line				= FRAME_HEIGHT;

	params->dsi.horizontal_sync_active			= 10;//4 60
	params->dsi.horizontal_backporch				= 10;//32 80
	params->dsi.horizontal_frontporch				= 10;//32 80
	params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
	params->dsi.PLL_CLOCK = 165;//240; 185;//this value must be in MTK suggested table  200
		params->dsi.ssc_disable					= 1;

#if 1		
		//add by tjf start 20160506
		params->dsi.esd_check_enable = 1;
		params->dsi.customization_esd_check_enable = 1;
		
		params->dsi.lcm_esd_check_table[0].cmd          = 0x0a;//0a
		params->dsi.lcm_esd_check_table[0].count        = 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9c;//9c
		
		params->dsi.noncont_clock=TRUE;
		params->dsi.noncont_clock_period=2;
		//add by tjf end
#endif
}




static void lcm_init(void)
{
	lcd_power_en(1);
	MDELAY(50);

	SET_RESET_PIN(1);
	MDELAY(20);
	SET_RESET_PIN(0);
	MDELAY(20);
	SET_RESET_PIN(1);
	MDELAY(120);

	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);

}


static void lcm_suspend(void)
{
	push_table(lcm_deep_sleep_mode_in_setting, sizeof(lcm_deep_sleep_mode_in_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_resume(void)
{
  /*       unsigned int lcd_id = 0;
      lcd_id = ((mt_get_gpio_in(GPIO_LCD_ID1_PIN) << 1) & 0x2) | mt_get_gpio_in(GPIO_LCD_ID2_PIN);
#if defined(BUILD_LK)
	printf("crystal_lcd_id ili9806e_fwvga_dsi_vdo_ivo_bx_fpc49800825c =%x\n", lcd_id);	
	printf("crystal_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
        printf("crystal_lcd_id_pin2 =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));	
#else
	printk("crystal_lcd_id ili9806e_fwvga_dsi_vdo_ivo_bx_fpc49800825c =%x\n", lcd_id);	
	printk("crystal_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
        printk("crystal_lcd_id_pin2 =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));	
#endif 

*/

	lcm_init();
//push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
}




static unsigned int lcm_compare_id(void)
{
#ifdef BUILD_LK
	unsigned int lcd_id = 0;

	lcd_power_en(1);
	MDELAY(50);
   	lcd_id = (mt_get_gpio_in(GPIO_LCD_ID1_PIN));

	printf("lcd_id anan =%x\n", lcd_id);	
	printf("lcd_id_pin1 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	

	return (lcd_id  == 1)?1:0;
#else
	return 1;
#endif
}

LCM_DRIVER jd9161ba_fwvga_dsi_vdo_ivo_ips_c050swyg_mx2150p_wpf_d5033_lcm_drv = 
{
      .name			= "jd9161ba_fwvga_dsi_vdo_ivo_ips_c050swyg_mx2150p_wpf_d5033",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
};
