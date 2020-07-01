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

#define FRAME_WIDTH  										(720)
#define FRAME_HEIGHT 										(1280)

#define REGFLAG_DELAY             							0XFC
#define REGFLAG_END_OF_TABLE      							0xFD   // END OF REGISTERS MARKER

#define LCM_ID	

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

		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);

		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x07);


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

{0xFF,3,{0x98,0x81,0x03}},

//GIP_1

{0x01,1,{0x00}},
{0x02,1,{0x00}},
{0x03,1,{0x73}},
{0x04,1,{0x00}},
{0x05,1,{0x00}},
{0x06,1,{0x0C}},
{0x07,1,{0x00}},
{0x08,1,{0x00}},
{0x09,1,{0x19}},
{0x0a,1,{0x01}},
{0x0b,1,{0x01}},
{0x0c,1,{0x0B}},
{0x0d,1,{0x01}},
{0x0e,1,{0x01}},
{0x0f,1,{0x19}},
{0x10,1,{0x1D}},
{0x11,1,{0x00}},
{0x12,1,{0x00}},
{0x13,1,{0x02}},
{0x14,1,{0x00}},
{0x15,1,{0x10}},
{0x16,1,{0x10}},
{0x17,1,{0x00}},
{0x18,1,{0x00}},
{0x19,1,{0x00}},
{0x1a,1,{0x00}},
{0x1b,1,{0x00}},
{0x1c,1,{0x00}},
{0x1d,1,{0x00}},
{0x1e,1,{0x44}},
{0x1f,1,{0xC0}},  
{0x20,1,{0x0A}},
{0x21,1,{0x03}},
{0x22,1,{0x0A}},
{0x23,1,{0x00}},
{0x24,1,{0x8C}},
{0x25,1,{0x8C}},
{0x26,1,{0x00}},
{0x27,1,{0x00}},
{0x28,1,{0x3B}},
{0x29,1,{0x03}},
{0x2a,1,{0x00}},
{0x2b,1,{0x00}},
{0x2c,1,{0x00}},
{0x2d,1,{0x00}},
{0x2e,1,{0x00}},
{0x2f,1,{0x00}},
{0x30,1,{0x00}},
{0x31,1,{0x00}},
{0x32,1,{0x00}},
{0x33,1,{0x00}},
{0x34,1,{0x00}},
{0x35,1,{0x00}},
{0x36,1,{0x00}},
{0x37,1,{0x00}},
{0x38,1,{0x00}},
{0x39,1,{0x00}},
{0x3a,1,{0x00}},
{0x3b,1,{0x00}},
{0x3c,1,{0x00}},
{0x3d,1,{0x00}},
{0x3e,1,{0x00}},
{0x3f,1,{0x00}},
{0x40,1,{0x00}},
{0x41,1,{0x00}},
{0x42,1,{0x00}},
{0x43,1,{0x00}},
{0x44,1,{0x00}},


//GIP_2
{0x50,1,{0x01}},
{0x51,1,{0x23}},
{0x52,1,{0x45}},
{0x53,1,{0x67}},
{0x54,1,{0x89}},
{0x55,1,{0xab}},
{0x56,1,{0x01}},
{0x57,1,{0x23}},
{0x58,1,{0x45}},
{0x59,1,{0x67}},
{0x5a,1,{0x89}},
{0x5b,1,{0xab}},
{0x5c,1,{0xcd}},
{0x5d,1,{0xef}},

//GIP_3
{0x5e,1,{0x11}},
{0x5f,1,{0x02}},
{0x60,1,{0x00}},
{0x61,1,{0x0C}},
{0x62,1,{0x0D}},
{0x63,1,{0x0E}},
{0x64,1,{0x0F}},
{0x65,1,{0x02}},
{0x66,1,{0x02}},
{0x67,1,{0x02}},
{0x68,1,{0x02}},
{0x69,1,{0x02}},
{0x6a,1,{0x02}},
{0x6b,1,{0x02}},
{0x6c,1,{0x02}},
{0x6d,1,{0x02}},
{0x6e,1,{0x05}},
{0x6f,1,{0x05}},
{0x70,1,{0x05}},
{0x71,1,{0x05}},
{0x72,1,{0x01}},
{0x73,1,{0x06}},
{0x74,1,{0x07}},
{0x75,1,{0x02}},
{0x76,1,{0x00}},
{0x77,1,{0x0C}},
{0x78,1,{0x0D}},
{0x79,1,{0x0E}},
{0x7a,1,{0x0F}},
{0x7b,1,{0x02}},
{0x7c,1,{0x02}},
{0x7d,1,{0x02}},
{0x7e,1,{0x02}},
{0x7f,1,{0x02}},
{0x80,1,{0x02}},
{0x81,1,{0x02}},
{0x82,1,{0x02}},
{0x83,1,{0x02}},
{0x84,1,{0x05}},
{0x85,1,{0x05}},
{0x86,1,{0x05}},
{0x87,1,{0x05}},
{0x88,1,{0x01}},
{0x89,1,{0x06}},
{0x8A,1,{0x07}},

//CMD_Page 4
{0xFF,3,{0x98,0x81,0x04}},
{0x6C,1,{0x15}},
{0x6E,1,{0x19}},               //di_pwr_reg=0 VGH clamp 15V
{0x6F,1,{0x25}},               // reg vcl + VGH pumping ratio 3x VGL=-2x            
{0x8D,1,{0x1F}},              //VGL clamp -10V
{0x87,1,{0xBA}},               
{0x26,1,{0x76}},
{0xB2,1,{0xD1}},

//CMD_Page 1
{0xFF,3,{0x98,0x81,0x01}},
{0x22,1,{0x0A}},		//BGR, SS
{0x31,1,{0x00}}, 
{0x40,1,{0x53}}, 
{0x43,1,{0x66}}, 
{0x53,1,{0x6A}},		//VCOM1
{0x55,1,{0x64}},		//VCOM2
{0x50,1,{0x97}},         	//VREG1OUT
{0x51,1,{0x92}},         	//VREG2OUT
{0x60,1,{0x15}},              //SDT
{0x61,1,{0x01}},  
{0x62,1,{0x0C}}, 
{0x63,1,{0x00}}, 

//VP255	Gamma P
{0xA0,1,{0x00}},
//VP251		
{0xA1,1,{0x11}},
//VP247                       
{0xA2,1,{0x1D}},
//VP243                       
{0xA3,1,{0x11}},
//VP239                       
{0xA4,1,{0x14}}, 
//VP231                       
{0xA5,1,{0x26}},
//VP219                       
{0xA6,1,{0x1B}},
//VP203                      
{0xA7,1,{0x1E}},
//VP175                       
{0xA8,1,{0x68}},
//VP144                       
{0xA9,1,{0x1D}},
//VP111                       
{0xAA,1,{0x28}},
//VP80                       
{0xAB,1,{0x54}},
//VP52                        
{0xAC,1,{0x17}},
//VP36                        
{0xAD,1,{0x15}},
//VP24                        
{0xAE,1,{0x49}},
//VP16                        
{0xAF,1,{0x1F}},
//VP12                        
{0xB0,1,{0x27}},
//VP8                        
{0xB1,1,{0x45}},
//VP4                         
{0xB2,1,{0x60}},
//VP0                          
{0xB3,1,{0x3F}},                        


//VN255 GAMMA N                                               
{0xC0,1,{0x00}},		
//VN251
{0xC1,1,{0x11}},
//VN247                       
{0xC2,1,{0x1D}},
//VN243                      
{0xC3,1,{0x11}},
//VN239                        
{0xC4,1,{0x14}}, 
//VN231                      
{0xC5,1,{0x26}},
//VN219                       
{0xC6,1,{0x1B}},
//VN203                       
{0xC7,1,{0x1E}},
//VN175                       
{0xC8,1,{0x68}},
//VN144                       
{0xC9,1,{0x1D}},
//VN111                       
{0xCA,1,{0x29}},
//VN80                       
{0xCB,1,{0x54}},
//VN52                        
{0xCC,1,{0x18}},
//VN36                        
{0xCD,1,{0x15}},
//VN24                        
{0xCE,1,{0x4A}},
//VN16                        
{0xCF,1,{0x1F}},
//VN12                        
{0xD0,1,{0x26}}, 
//VN8                       
{0xD1,1,{0x46}},
//VN4                         
{0xD2,1,{0x60}},
//VN0                         
{0xD3,1,{0x3F}},              



//CMD_Page 0
{0xFF,3,{0x98,0x81,0x00}},
{0x35,1,{0x00}},

//{0x35,1,{0x77}},
{0x11,1,{0x00}},
{REGFLAG_DELAY, 120, {}},
{0x29,1,{0x00}},
{REGFLAG_DELAY,100,{}}, 			  // Display On
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

		params->dsi.mode   = BURST_VDO_MODE;

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				= LCM_FOUR_LANE;
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

	params->dsi.vertical_sync_active				= 4;//2;
	params->dsi.vertical_backporch					= 20;   // from Q driver
	params->dsi.vertical_frontporch					= 24;  // rom Q driver
	params->dsi.vertical_active_line				= FRAME_HEIGHT;

	params->dsi.horizontal_sync_active				= 40;//10;
	params->dsi.horizontal_backporch				= 100; // from Q driver
	params->dsi.horizontal_frontporch				= 70;  // from Q driver
	params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
	params->dsi.PLL_CLOCK = 221;//240; //this value must be in MTK suggested table
		params->dsi.ssc_disable					= 1;
}




static void lcm_init(void)
{

	lcd_power_en(1);
    MDELAY(100);
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
#ifndef BUILD_LK
	printk("crystal_lcd_yxd");		
#endif 

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

	return (lcd_id  == 0)?1:0;
#else
	return 1;
#endif
}

LCM_DRIVER ili9881c_hd720_dsi_vdo_boe_hlt_ht55061_mx2109f_l7p_lcm_drv = 
{
      .name			= "ili9881c_hd720_dsi_vdo_boe_hlt_ht55061_mx2109f_l7p",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
};
