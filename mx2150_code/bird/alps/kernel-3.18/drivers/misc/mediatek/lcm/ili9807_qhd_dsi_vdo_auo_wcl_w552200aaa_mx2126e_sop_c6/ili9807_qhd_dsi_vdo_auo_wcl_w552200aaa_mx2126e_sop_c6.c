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

#define FRAME_WIDTH  										(540)
#define FRAME_HEIGHT 										(960)

#define REGFLAG_DELAY             							0XFFE
#define REGFLAG_END_OF_TABLE      							0xFFF   // END OF REGISTERS MARKER

#define LCM_ID	0x9605

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
    unsigned char para_list[64];
};


static struct LCM_setting_table lcm_initialization_setting[] = {
//ILI9807+H546AAN01.1 OK 2015-11-23 GAN
{0xFF,5,{0xFF,0x98,0x07,0x00,0x01}},
{0x31,1,{0x08}},
{0x40, 1, {0x50}}, 

{0x41, 1, {0x33}}, 

{0x42, 1, {0x01}},

{0x44, 1, {0x06}}, 

{0x46, 1, {0xCC}},              // DDVDL pumping clock * 1

{0x47, 1, {0xCC}},
            
{0x52, 1, {0x00}},

{0x53, 1, {0x45}},             // vcom  

{0x60,1,{0x05}}, 
{0x61,1,{0x08}}, 
{0x62,1,{0x08}}, 
{0x63,1,{0x08}}, 
{0x64,1,{0x88}},

//++++++++++++++++++ Gamma2.2 Setting ++++++++++++++++++//
{0xA0, 1, {0x00}},        //255
{0xA1, 1, {0x05}},        //251
{0xA2, 1, {0x0A}},        //247
{0xA3, 1, {0x0B}},        //239
{0xA4, 1, {0x03}},        //231
{0xA5, 1, {0x03}},        //203
{0xA6, 1, {0x06}},        //175
{0xA7, 1, {0x02}},        //147
{0xA8, 1, {0x0B}},        //108
{0xA9, 1, {0x0D}},        //80
{0xAA, 1, {0x18}},        //52
{0xAB, 1, {0x00}},        //24
{0xAC, 1, {0x00}},        //16
{0xAD, 1, {0x00}},        //8
{0xAE, 1, {0x01}},        //4
{0xAF, 1, {0x00}},        //0

{0xC0, 1, {0x00}},        //255
{0xC1, 1, {0x06}},        //251
{0xC2, 1, {0x09}},        //247
{0xC3, 1, {0x0A}},        //239
{0xC4, 1, {0x03}},        //231
{0xC5, 1, {0x03}},        //203
{0xC6, 1, {0x05}},        //175
{0xC7, 1, {0x03}},        //147
{0xC8, 1, {0x0B}},        //108
{0xC9, 1, {0x0D}},        //80
{0xCA, 1, {0x19}},        //52
{0xCB, 1, {0x00}},        //24
{0xCC, 1, {0x00}},        //16
{0xCD, 1, {0x00}},        //8
{0xCE, 1, {0x01}},        //4
{0xCF, 1, {0x00}},        //0

//****************************************************************************//
//****************************** Page 6 Command ******************************//
//****************************************************************************//
{0xFF,5,{0xFF,0x98,0x07,0x00,0x06}},
{0x00,1,{0x21}}, 
{0x01,1,{0x02}}, 
{0x02,1,{0x84}}, 
{0x03,1,{0x00}}, 
{0x04,1,{0x16}}, 
{0x05,1,{0x16}}, 
{0x06,1,{0x80}}, 
{0x07,1,{0x06}}, 
{0x08,1,{0x00}}, 
{0x09,1,{0x10}}, 
{0x0A,1,{0x00}}, 
{0x0B,1,{0x00}}, 
{0x0C,1,{0x16}}, 
{0x0D,1,{0x16}},  
{0x0E,1,{0x00}}, 
{0x0F,1,{0x00}}, 

{0x10,1,{0x55}}, 
{0x11,1,{0x50}}, 
{0x12,1,{0x00}}, 
{0x13,1,{0x00}}, 
{0x14,1,{0x00}}, 
{0x15,1,{0xC0}}, 
{0x16,1,{0x08}}, 
{0x17,1,{0x00}}, 
{0x18,1,{0x00}}, 
{0x19,1,{0x00}}, 
{0x1A,1,{0x0F}}, 
{0x1B,1,{0x16}}, 
{0x1C,1,{0x00}}, 
{0x1D,1,{0x00}}, 

{0x20,1,{0x01}}, 
{0x21,1,{0x23}}, 
{0x22,1,{0x45}}, 
{0x23,1,{0x67}}, 
{0x24,1,{0x01}}, 
{0x25,1,{0x23}}, 
{0x26,1,{0x45}}, 
{0x27,1,{0x67}}, 

{0x30,1,{0x10}}, 
{0x31,1,{0x22}}, 
{0x32,1,{0x22}}, 
{0x33,1,{0x86}}, 
{0x34,1,{0xDD}}, 
{0x35,1,{0xCC}}, 
{0x36,1,{0xBA}}, 
{0x37,1,{0xAB}}, 
{0x38,1,{0x68}}, 
{0x39,1,{0x22}}, 
{0x3A,1,{0x22}}, 
{0x3B,1,{0x22}}, 
{0x3C,1,{0x22}}, 

{0x3D,1,{0x22}}, 
{0x3E,1,{0x22}}, 
{0x3F,1,{0x22}}, 
{0x40,1,{0x22}}, 
{0x41,1,{0x22}}, 
{0x42,1,{0x22}}, 
{0x43,1,{0x22}}, 
{0x58,1,{0x47}},

//****************************************************************************//
//****************************** Page 7 Command ******************************//
//****************************************************************************//
{0xFF,5,{0xFF,0x98,0x07,0x00,0x07}},
{0x02,1,{0x77}}, 
{0xE7,1,{0x01}},

//****************************************************************************//
//****************************** Page 0 Command ******************************//
//****************************************************************************//
{0xFF,5,{0xFF,0x98,0x07,0x00,0x00}},
{0x35,1,{0x00}},
{0x11,1,{0x00}},
{REGFLAG_DELAY, 120, {}},
{0x29,1,{0x00}},
{REGFLAG_DELAY, 120, {}},
	

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

		params->dsi.mode   = SYNC_PULSE_VDO_MODE;

	
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
		params->dsi.intermediat_buffer_num = 2;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

		params->dsi.vertical_sync_active				= 4;
		params->dsi.vertical_backporch					= 16;
		params->dsi.vertical_frontporch					= 15;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 8;//10
		params->dsi.horizontal_backporch				= 64;
		params->dsi.horizontal_frontporch				= 64;
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
		params->dsi.PLL_CLOCK = 230;//210;
		params->dsi.ssc_disable					= 1;
}




static void lcm_init(void)
{
	lcd_power_en(1);
    SET_RESET_PIN(1);
    SET_RESET_PIN(0);
    MDELAY(10);
    SET_RESET_PIN(1);
    MDELAY(10);

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
/*
	unsigned int lcd_id = 0;

	lcd_id = mt_get_gpio_in(GPIO_LCD_ID1_PIN) ;
#if defined(BUILD_LK)
	printf("crystal_lcd_id =%x\n", lcd_id);	
	printf("crystal_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
#else
	printk("crystal_lcd_id_hst =%x\n", lcd_id);	
	printk("crystal_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
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
	//lcd_id = mt_get_gpio_in(GPIO_LCD_ID1_PIN);
        lcd_id = ((mt_get_gpio_in(GPIO_LCD_ID1_PIN) << 1) & 0x2) | mt_get_gpio_in(GPIO_LCD_ID2_PIN);

	printf("crystal_lcd_id =%x\n", lcd_id);	
	printf("ili9807_qhd_dsi_vdo_auo_wcl_w552055vb_mx1091d_koomi_fot9_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
        printf("ili9807_qhd_dsi_vdo_auo_wcl_w552055vb_mx1091d_koomi_fot9_pin2 =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));	
 
	return (lcd_id == 0)?1:0;
#else
	return 1;
#endif

}


LCM_DRIVER ili9807_qhd_dsi_vdo_auo_wcl_w552200aaa_mx2126e_sop_c6_lcm_drv = 
{
    .name			= "ili9807_qhd_dsi_vdo_auo_wcl_w552200aaa_mx2126e_sop_c6",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};

