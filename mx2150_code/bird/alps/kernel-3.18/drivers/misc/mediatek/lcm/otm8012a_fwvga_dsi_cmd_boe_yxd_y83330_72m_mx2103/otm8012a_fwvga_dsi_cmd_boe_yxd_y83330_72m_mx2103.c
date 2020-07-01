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

#define FRAME_WIDTH  									(480)
#define FRAME_HEIGHT 									(854)
#define LCM_ID                								(0x8012)
#define REGFLAG_DELAY             							 0xFE
#define REGFLAG_END_OF_TABLE      							0xFFF   // END OF REGISTERS MARKER

// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    								(lcm_util.set_reset_pin((v)))

#define UDELAY(n) 									(lcm_util.udelay(n))
#define MDELAY(n) 									(lcm_util.mdelay(n))


// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)						lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)			lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg						lcm_util.dsi_read_reg()
#define read_reg_v2(cmd, buffer, buffer_size)   		lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)           

//#define LCM_DSI_CMD_MODE   1
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

{0x00,1,{0x00}},{0xFF,3,{0x80,0x12,0x01}},
{0x00,1,{0x80}},{0xFF,2,{0x80,0x12}},
{0x00,1,{0x90}},{0xB3,1,{0x02}},
{0x00,1,{0xB0}},{0xB3,1,{0x18}},
{0x00,1,{0x80}},{0xC1,2,{0x24,0x044}},
{0x00,1,{0x81}},{0xC0,1,{0x57}},

{0x00,1,{0x83}},{0xb2,1,{0x10}},

{0x00,1,{0x81}},{0xC1,1,{0x44}},//

{0x00,1,{0x91}},{0xC5,1,{0x76}},
{0x00,1,{0x00}},{0xD8,2,{0x70,0x70}},
{0x00,1,{0x00}},{0xD9,1,{0x3f}},
{0x00,1,{0x92}},{0xC5,1,{0x01}},
{0x00,1,{0x94}},{0xC5,2,{0x44,0x44}},
{0x00,1,{0x80}},{0xC5,1,{0x50}},
{0x00,1,{0x81}},{0xC4,1,{0x04}},
{0x00,1,{0x96}},{0xC5,1,{0x46}},
{0x00,1,{0x90}},{0xC5,1,{0x02}},
{0x00,1,{0xBA}},{0xF5,2,{0x01,0x11}},
{0x00,1,{0xB0}},{0xB3,1,{0x18}},
{0x00,1,{0xA1}},{0xB3,1,{0x00}},
{0x00,1,{0x92}},{0xC4,1,{0x00}},
{0x00,1,{0xB4}},{0xC0,1,{0x55}},
{0x00,1,{0x82}},{0xC4,1,{0xF2}},
{0x00,1,{0xC2}},{0xF5,1,{0x00}},
{0x00,1,{0x82}},{0xC5,1,{0xF0}},
{0x00,1,{0xA3}},{0xC0,2,{0x05,0x15}},
{0x00,1,{0xC7}},{0xCF,1,{0x80}},
{0x00,1,{0xC9}},{0xCF,2,{0x0A,0x06}},
{0x00,1,{0x8B}},{0xC4,1,{0x24}},
                        
{0x00,1,{0x80}},{0xCE,12,{0x85,0x01,0x00,0x84,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},  
{0x00,1,{0xA0}},{0xCE,14,{0x18,0x04,0x03,0x59,0x00,0x00,0x00,0x18,0x03,0x03,0x5A,0x00,0x00,0x00}},  
{0x00,1,{0xB0}},{0xCE,14,{0x18,0x02,0x03,0x57,0x00,0x00,0x00,0x18,0x01,0x03,0x58,0x00,0x00,0x00}},  
{0x00,1,{0xC0}},{0xCF,10,{0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x00,0x00}},
{0x00,1,{0xD0}},{0xCF,1,{0x00}},
{0x00,1,{0x80}},{0xCB,10,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
{0x00,1,{0x90}},{0xCB,15,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}}, 
{0x00,1,{0xA0}},{0xCB,15,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
{0x00,1,{0xB0}},{0xCB,10,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
{0x00,1,{0xC0}},{0xCB,15,{0x00,0x04,0x04,0x04,0x04,0x04,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},  
{0x00,1,{0xD0}},{0xCB,15,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},  
{0x00,1,{0xE0}},{0xCB,10,{0x04,0x04,0x04,0x04,0x04,0x00,0x00,0x00,0x00,0x00}}, 
{0x00,1,{0xF0}},{0xCB,10,{0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF}},
{0x00,1,{0x80}},{0xCC,10,{0x00,0x26,0x09,0x0B,0x01,0x25,0x00,0x00,0x00,0x00}},
{0x00,1,{0x90}},{0xCC,15,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}}, 
{0x00,1,{0xA0}},{0xCC,15,{0x00,0x00,0x00,0x00,0x00,0x25,0x02,0x0C,0x0A,0x26,0x00,0x00,0x00,0x00,0x00}},
{0x00,1,{0xB0}},{0xCC,10,{0x00,0x25,0x0C,0x0A,0x02,0x26,0x00,0x00,0x00,0x00}}, 
{0x00,1,{0xC0}},{0xCC,15,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
{0x00,1,{0xD0}},{0xCC,17,{0x00,0x00,0x00,0x00,0x00,0x25,0x0B,0x09,0x01,0x26,0x00,0x00,0x00,0x00,0x00,0x11,0x00}},
{0x00,1,{0x00}},{0x3A,1,{0x77}},

{0x00,1,{0x00}},
{0xE1,20,{0x09,0x1C,0x29,0x37,0x44,0x54,0x56,0x81,0x73,0x8C,0x75,0x5F,0x70,0x4E,0x4C,0x3F,0x33,0x25,0x18,0x10}},
 
{0x00,1,{0x00}},
{0xE2,20,{0x09,0x1C,0x29,0x37,0x45,0x54,0x56,0x82,0x73,0x8C,0x75,0x5F,0x70,0x4E,0x4B,0x3E,0x34,0x25,0x18,0x10}}, 
{0x00,1,{0x00}},
{0x11,1,{0x00}},
{REGFLAG_DELAY,100,{}},
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}},
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}},
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}},
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}},
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}},
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}},
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}},
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x08}}, {REGFLAG_DELAY,10,{}}, 
{0x00,1,{0x92}},{0xC4,1,{0x00}}, {REGFLAG_DELAY,10,{}}, 

{0x00,1,{0x92}},{0xC4,1,{0x08}},

{0x00,1,{0x00}},{0xFF,3,{0xFF,0xFF,0xFF}},
{0x00,1,{0x00}},{0x35,	1,	{0x00}},
//{0x00,1,{0x00}},{0x44,	2,	{((100)>>8), (100&0xFF)}},
{0x00,1,{0x00}},{0x44,2,{0x00, 0x00}},//64
{0x00,1,{0x00}},{0x29,1,{0x00}},
{REGFLAG_DELAY,10,{}},
{0x2C,1,{0x00}},
{REGFLAG_END_OF_TABLE,0x00,{}}

};

#if 0
static struct LCM_setting_table lcm_set_window[] = {
	{0x2A,	4,	{0x00, 0x00, (FRAME_WIDTH>>8), (FRAME_WIDTH&0xFF)}},
	{0x2B,	4,	{0x00, 0x00, (FRAME_HEIGHT>>8), (FRAME_HEIGHT&0xFF)}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};

static struct LCM_setting_table lcm_sleep_out_setting[] = {


// Sleep Out
	{0x11, 1, {0x00}},
	{REGFLAG_DELAY, 60, {}},
// Display ON
	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
#endif
static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
	// Display off sequence
	{0x28, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},
    // Sleep Mode On
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
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_VSYNC_ONLY;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = CMD_MODE;
		//params->dsi.mode   =  SYNC_PULSE_VDO_MODE;  //BURST_VDO_MODE;

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

	//	params->dsi.word_count=480*3;	
		params->dsi.vertical_sync_active=4;
		params->dsi.vertical_backporch=20;
		params->dsi.vertical_frontporch=20;
		params->dsi.vertical_active_line=854;
	
	//	params->dsi.line_byte=2180;		// 2256 = 752*3
		params->dsi.horizontal_sync_active				= 10;
    params->dsi.horizontal_backporch				= 80; //60
    params->dsi.horizontal_frontporch				= 80; //200
    params->dsi.horizontal_blanking_pixel				= 60; //
    params->dsi.horizontal_active_pixel				= FRAME_WIDTH;
	//	params->dsi.rgb_byte=(480*3+6);	
	
	//	params->dsi.horizontal_sync_active_word_count=20;	
	//	params->dsi.horizontal_backporch_word_count=200;
	//	params->dsi.horizontal_frontporch_word_count=200;

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
    	MDELAY(10);
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
#ifndef BUILD_LK
	printk("====fang_yixingda====\n");
#endif
lcm_init();
	//push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_update(unsigned int x, unsigned int y,
                       unsigned int width, unsigned int height)
{
	unsigned int x0 = x;
	unsigned int y0 = y;
	unsigned int x1 = x0 + width - 1;
	unsigned int y1 = y0 + height - 1;

	unsigned char x0_MSB = ((x0>>8)&0xFF);
	unsigned char x0_LSB = (x0&0xFF);
	unsigned char x1_MSB = ((x1>>8)&0xFF);
	unsigned char x1_LSB = (x1&0xFF);
	unsigned char y0_MSB = ((y0>>8)&0xFF);
	unsigned char y0_LSB = (y0&0xFF);
	unsigned char y1_MSB = ((y1>>8)&0xFF);
	unsigned char y1_LSB = (y1&0xFF);

	unsigned int data_array[16];

    data_array[0]= 0x00053902; 
    data_array[1]= (x1_MSB<<24)|(x0_LSB<<16)|(x0_MSB<<8)|0x2a; 
    data_array[2]= (x1_LSB); 
    dsi_set_cmdq(data_array, 3, 1);  

    data_array[0]= 0x00053902; 
    data_array[1]= (y1_MSB<<24)|(y0_LSB<<16)|(y0_MSB<<8)|0x2b; 
    data_array[2]= (y1_LSB); 
    dsi_set_cmdq(data_array, 3, 1);  

    data_array[0]= 0x002c3909; //high speed transfer 
    //data_array[6]= 0x002c3901;  //low power transfer  
    dsi_set_cmdq(data_array, 1, 0); 
}



static unsigned int lcm_compare_id(void)
{
	unsigned int id = 0;
	unsigned char buffer[5];
	unsigned int array[16];

    lcd_power_en(1);
    MDELAY(50);
    
    SET_RESET_PIN(1);  //NOTE:should reset LCM firstly
    MDELAY(10);
    SET_RESET_PIN(0);
    MDELAY(20);
    SET_RESET_PIN(1);
    MDELAY(20);

	//push_table(lcm_compare_id_setting, sizeof(lcm_compare_id_setting) / sizeof(struct LCM_setting_table), 1);

	array[0] = 0x00053700;// read id return two byte,version and id
	dsi_set_cmdq(array, 1, 1);
	read_reg_v2(0xA1, buffer, 5);
	id = ((buffer[2] << 8)& 0xff00)|buffer[3]; //we only need ID

    	#if defined(BUILD_LK)
	printf("LCM_ID should 8012tftf be %x, read_id = 0x%x\n", LCM_ID, id);
 	printf("LCM_ID should 8012tftf be %x, read_id = 0x%x\n", LCM_ID, id);
 	#else
        printk("LCM_ID should 8012 tftf be %x, read_id = 0x%x\n", LCM_ID, id);
        printk("LCM_ID should 8012 tftf be %x, read_id = 0x%x\n", LCM_ID, id);
	#endif
	return (LCM_ID == id)?1:0;

/*
	unsigned int lcd_id = 0;

   lcd_id = ((mt_get_gpio_in(GPIO_LCD_ID1_PIN) << 1) & 0x2) | mt_get_gpio_in(GPIO_LCD_ID2_PIN);

#if defined(BUILD_LK)
    printf("super_lcd_id =%x\n", lcd_id);	
    printf("super_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
    printf("super_lcd_id_pin2 =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));
#else
    printk("super_lcd_id =%x\n", lcd_id);	
    printk("super_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
    printk("super_lcd_id_pin2 =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));
#endif 

	return (lcd_id  == 0)?1:0;
*/
//	return 1;
}

LCM_DRIVER otm8012a_fwvga_dsi_cmd_boe_yxd_y83330_72m_mx2103_lcm_drv = 
{
      .name			= "otm8012a_fwvga_dsi_cmd_boe_yxd_y83330_72m_mx2103",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.update         = lcm_update,

	//.set_pwm        = lcm_setpwm,
	//.get_pwm        = lcm_getpwm
	.compare_id    = lcm_compare_id,
};
