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

#define FRAME_WIDTH                                                                                  (480) 
#define FRAME_HEIGHT                                                                                 (854) 

#define REGFLAG_DELAY             							0XFFE
#define REGFLAG_END_OF_TABLE      							0xFFF   // END OF REGISTERS MARKER

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

		//pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		//pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);
	}	
	else
	{
		//pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	}
	MDELAY(20);

}


struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[64];
};


static struct LCM_setting_table lcm_initialization_setting[] = {
///mx2101_t5g project
 {0xB9,3,{0xF1,0x08,0x01}},	

{0xB1,4,{0x22,0x1E,0x1E,0x87}},	

{0xB2,1,{0x22}},	//480x854

{0xB3,8,{0x00,0x00,0x06,0x06,0x20,0x20,0x30,0x30}},	//HFP_GEN

{0xBA,17,{0x31,0x00,0x44,0x25,0x91,0x0A,0x00,0x00,0xC1,0x00,0x00,0x00,0x0D,0x02,0x4F,0xB9,0xEE}},	//17

{0xE3,5,{0x04,0x04,0x01,0x01,0x00}},  //	

{0xB4,1,{0x00}},  //02

{0xB5,2,{0x07,0x07}},  //	0a

{0xB6,2,{0x39,0x39}},	//VCOM OFFSET //2E40  43 3f 32 //43  39 

{0xB8,2,{0x64,0x20}},  //2x, 2.5x	

{0xCC,1,{0x02}},  //02	

{0xBC,1,{0x47}},  //	

{0xE9,51,{0x00,0x00,0x06,0x00,0x00,0x81,0x89,0x12,0x31,0x23,0x23,0x08,0x81,0x80,0x23,0x00,0x00,0x10,0x00,0x00,0x00,0x0f,0x89,
                  0x13,0x18,0x88,0x88,0x88,0x88,0x88,0x88,0x89,0x02,0x08,0x88,0x88,0x88,0x88,0x88,0x88,0x00,0x00,0x00,0x00,0x00,
                  0x00,0x00,0x00,0x00,0x00,0x00}},	//53//CKS 

{0xEA,22,{0x90,0x00,0x00,0x00,0x88,0x02,0x09,0x88,0x88,0x88,0x88,0x88,0x88,0x88,0x13,0x19,0x88,0x88,0x88,0x88,0x88,0x88}},   //22COSR17,COS18     	

{0xE0,34,{0x00,0x16,0x0b,0x36,0x3F,0x3F,0x28,0x41,0x08,0x0D,0x0E,0x11,0x13,0x10,0x11,0xE,0x16,
         0x00,0x16,0x0b,0x36,0x3F,0x3F,0x28,0x41,0x08,0x0D,0x0E,0x11,0x13,0x10,0x11,0xE,0x16}},   //34

{0x11, 1, {0x00}},
{REGFLAG_DELAY, 120, {}},

{0x29, 1, {0x00}},
{REGFLAG_DELAY, 20, {}},

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

static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
	// Sleep Out
	{0x28, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},

	// Display ON
	{0x10, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
#endif

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
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_VSYNC_ONLY;//LCM_DBI_TE_MODE_DISABLED;  
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = SYNC_PULSE_VDO_MODE; //BURST_VDO_MODE;//

	
		// DSI
		/* Command mode setting */
	params->dsi.LANE_NUM				= LCM_TWO_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
		params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
		params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
		
		params->dsi.word_count=FRAME_WIDTH*3;
		params->dsi.vertical_active_line=FRAME_HEIGHT;
						
		// Highly depends on LCD driver capability.
		// Not support in MT6573
		//params->dsi.packet_size=256;

		// Video mode setting		
		params->dsi.intermediat_buffer_num = 2;


		params->dsi.vertical_sync_active				= 4;//4;
		params->dsi.vertical_backporch				= 10;   // from Q driver 10
		params->dsi.vertical_frontporch				= 9;  // rom Q driver 18
		params->dsi.vertical_active_line				= FRAME_HEIGHT;

		params->dsi.horizontal_sync_active			= 31;//10;3
		params->dsi.horizontal_backporch				= 31; // from Q driver
		params->dsi.horizontal_frontporch				= 31;  // from Q driver
		params->dsi.horizontal_active_pixel			= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=1;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =28;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
        params->dsi.PLL_CLOCK = 190;//221 //240; //this value must be in MTK suggested table
}




static void lcm_init(void)
{
	lcd_power_en(1);
	MDELAY(50);
	SET_RESET_PIN(1);
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
	MDELAY(120);

    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);

}

static void lcm_suspend(void)
{
	unsigned int data_array[16];

	data_array[0]=0x00280500; // Display Off
	dsi_set_cmdq(data_array, 1, 1);
	
	data_array[0] = 0x00100500; // Sleep In
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(120); 

	
	SET_RESET_PIN(1);	
	SET_RESET_PIN(0);
	MDELAY(20); // 1ms
	
	SET_RESET_PIN(1);
	MDELAY(120); 
}


static void lcm_resume(void)
{
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


LCM_DRIVER fl10802_fwvga_dsi_vdo_boe_jj_fpct50v254v1_mx2109f_ares_a5085_for_mx2132er_lcm_drv = 
{
    .name			= "fl10802_fwvga_dsi_vdo_boe_jj_fpct50v254v1_mx2109f_ares_a5085_for_mx2132er",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
};
