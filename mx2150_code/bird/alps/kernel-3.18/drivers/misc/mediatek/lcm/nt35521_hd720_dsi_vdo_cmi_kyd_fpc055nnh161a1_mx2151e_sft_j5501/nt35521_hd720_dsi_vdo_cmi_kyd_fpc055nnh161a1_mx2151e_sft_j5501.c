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

		//pmic_set_register_value(PMIC_RG_VGP1_EN,1);//2.8v
		//pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);//1.8v
	}	
	else
	{
		//pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,0);//1.8v
	}
	MDELAY(20);

}

struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[64];
};


static struct LCM_setting_table lcm_sleep_in_setting[] = {
	// Display off sequence
	{0x28, 1, {0x00}},
	{REGFLAG_DELAY, 10, {}},
    // Sleep Mode On
	{0x10, 1, {0x00}},
	{REGFLAG_DELAY, 100, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
static struct LCM_setting_table lcm_initialization_setting[] = {
//========== Page 0 relative ==========
{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},
{0xB1,2,{0x68,0x21}},
// ##NL, (1280-480)/4=800=C8h
{0xB5,2,{0xC8,0x00}},
// ##Inversion = Column inversion
{0xBC,1,{0x00}},
// ##RTN, T1A, VBP, VFP
{0xBD,5,{0xA1,0xAD,0x10,0x10,0x01}},
// ##Watch dog
{0xC8,1,{0x80}},

//========== Page 1 relative ==========
{0xF0,5,{0x55,0xAA,0x52,0x08,0x01}},
// ##VGH=16V
{0xB3,1,{0x2B}},
// ##VGL=-12V
{0xB4,1,{0x19}},
// ##VGH & VGL control
{0xBB,1,{0x05}},
// ##VGMP=4.8V
{0xBC,1,{0x90}},
// ##VGMN=-4.8V
{0xBD,1,{0x90}},
// ##VCOM
{0xBE,1,{0x51}},

//========== Page 2 relative ==========
//2017/4/6									
{0xF0,5,{0x55,0xAA,0x52,0x08,0x02}},
//DGMA_SEL[1:0] = 1									
{0xEE,1,{0x01}},
//Gamma Setting									
{0xB0,16,{0x00,0x00,0x00,0x11,0x00,0x32,0x00,0x4E,0x00,0x65,0x00,0x79,0x00,0x8A,0x00,0x9A}},
{0xB1,16,{0x00,0xA9,0x00,0xDD,0x01,0x07,0x01,0x4B,0x01,0x82,0x01,0xE0,0x02,0x31,0x02,0x33}},
{0xB2,16,{0x02,0x7C,0x02,0xCA,0x02,0xF8,0x03,0x31,0x03,0x58,0x03,0x8B,0x03,0x98,0x03,0xA8}},
{0xB3,12,{0x03,0xBB,0x03,0xCD,0x03,0xDA,0x03,0xFE,0x03,0xF4,0x03,0xFF}},
//Offset Setting									
{0xE9,10,{0x8F,0x63,0xB0,0x41,0x00,0x8F,0x63,0xB0,0x41,0x00}},
{0xEA,10,{0x8F,0x63,0xB0,0x41,0x00,0x8F,0x63,0xB0,0x41,0x00}},
{0xEB,10,{0x8F,0x63,0xB0,0x41,0x00,0x8F,0x63,0xB0,0x41,0x00}},
																
//========== Page 3 relative ==========
{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},
// ##STV01
{0xB2,5,{0x00,0x05,0x02,0x00,0x00}},
// ##RST01
{0xB6,5,{0x00,0x82,0xA1,0x00,0x00}},
// ##RST02
{0xB7,5,{0x00,0xA2,0xA5,0x00,0x00}},
// ##CLK01
{0xBA,4,{0x48,0x00,0x00,0x00}},
// ##CLK02
{0xBB,4,{0x48,0x00,0x00,0x00}},
// ##VAC01
{0xC0,3,{0x00,0x0A,0x0A}},
// ##VAC02
{0xC1,3,{0x00,0x0A,0x0A}},

//========== Page 4 relative ==========	
{0xF0,5,{0x55,0xAA,0x52,0x08,0x04}},
// ##GPIO0=TE, GPIO1=LEDPWM
{0xB1,5,{0x02,0x03,0x00,0x15,0x16}},
{0xD3,1,{0x01}},

//========== Page 5 relative ==========	
{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},
// ##STV ON_Seq,
{0xB0,1,{0x06}},
// ##RST ON_Seq,
{0xB1,1,{0x06}},
// ##CLK ON_Seq,
{0xB2,2,{0x06,0x00}},
// ##VDC01 ON_Seq,
{0xB3,5,{0x06,0x00,0x00,0x00,0x00}},
// ##VAC ON_Seq,
{0xB7,3,{0x06,0x00,0x00}},
// ##IP Inverse
{0xBC,4,{0x00,0x00,0x00,0x01}},
// ##IP Enable
{0xBD,5,{0x03,0x01,0x03,0x03,0x01}},
// ##CLK01
{0xC0,2,{0x03,0x30}},
// ##CLK02
{0xC1,2,{0x03,0x31}},
// ##VAC01
{0xC4,3,{0x00,0x00,0x3C}},
// ##VAC02
{0xC5,3,{0x00,0x00,0x3C}},
// ##CLK01, Proch
{0xD1,5,{0x00,0x05,0x07,0x00,0x00}},
// ##CLK02, Proch
//{0xD2,5,{0x00,0x05,0x0D,0x00,0x00}},
{0xD2,5,{0x00,0x25,0x09,0x00,0x00}},
// ##Abnormal, 2 frame
{0xE3,1,{0x84}},
// ##CLK
{0xE5,1,{0x1A}},
// ##STV
{0xE6,1,{0x1A}},
// ##RST
{0xE7,1,{0x1A}},
// ##VAC
{0xE8,1,{0x1A}},
// ##VDC
{0xE9,1,{0x1A}},

//========== Page 6 relative ==========	
{0xF0,5,{0x55,0xAA,0x52,0x08,0x06}},
{0xB0,5,{0x2C,0x2D,0x10,0x12,0x14}},
{0xB1,5,{0x18,0x1A,0x1C,0x08,0x0A}},
{0xB2,5,{0x35,0x35,0x35,0x35,0x35}},
{0xB3,5,{0x35,0x35,0x00,0x35,0x35}},
{0xB4,5,{0x35,0x35,0x01,0x35,0x30}},
{0xB5,5,{0x35,0x35,0x35,0x35,0x35}},
{0xB6,5,{0x0B,0x09,0x1D,0x1B,0x19}},
{0xB7,5,{0x15,0x13,0x11,0x2D,0x2C}},
{0xC0,5,{0x2C,0x2D,0x19,0x15,0x13}},
{0xC1,5,{0x11,0x1D,0x1B,0x01,0x0B}},
{0xC2,5,{0x35,0x35,0x35,0x35,0x35}},
{0xC3,5,{0x35,0x35,0x09,0x35,0x35}},
{0xC4,5,{0x35,0x35,0x08,0x35,0x30}},
{0xC5,5,{0x35,0x35,0x35,0x35,0x35}},
{0xC6,5,{0x0A,0x00,0x1A,0x1C,0x10}},
{0xC7,5,{0x12,0x14,0x18,0x2D,0x2C}},
{0xD1,4,{0x30,0x30,0x30,0x30}},
{0xD2,4,{0x35,0x35,0x35,0x35}},

//========== CMD 2 Disable ==========
{0xF0,5,{0x55,0xAA,0x52,0x00,0x00}},

//ccmoff
//ccmrun


// ##TE output
{0x35,1,{0x00}},

//============Normal Display===========	
{0x11,1,{0x00}},
{REGFLAG_DELAY, 50, {}},
{0x29,1,{0x00}},
//BIST MODE
//{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},
//{0xEE,4,{0x87,0x78,0x02,0x40}},                                                                                                                          
                                                                                                                       

{REGFLAG_DELAY,10,{}},


{REGFLAG_END_OF_TABLE,0x00,{}}
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
		//params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

    	params->dsi.mode   = SYNC_PULSE_VDO_MODE;

	
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

		params->dsi.intermediat_buffer_num = 0;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
		params->dsi.word_count=720*3;	

		
		params->dsi.vertical_sync_active				= 4;
		params->dsi.vertical_backporch				= 16;
		params->dsi.vertical_frontporch 				= 16;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 4;
		params->dsi.horizontal_backporch				= 32;
		params->dsi.horizontal_frontporch				= 26;
		params->dsi.horizontal_active_pixel 			= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
		params->dsi.PLL_CLOCK = 205;//240; //this value must be in MTK suggested table

#if 0
		params->dsi.ssc_disable					= 0;
		//params->dsi.ssc_range = 3;
	//params->dsi.clk_lp_per_line_enable = 0;

#endif
#if 0
		/* ESD or noise interference recovery For video mode LCM only. */ // Send TE packet to LCM in a period of n frames and check the response. 
		params->dsi.lcm_int_te_monitor = FALSE; 
		params->dsi.lcm_int_te_period = 1; // Unit : frames 
 
		// Need longer FP for more opportunity to do int. TE monitor applicably. 
		if(params->dsi.lcm_int_te_monitor) 
			params->dsi.vertical_frontporch *= 2; 
 
		// Monitor external TE (or named VSYNC) from LCM once per 2 sec. (LCM VSYNC must be wired to baseband TE pin.) 
		params->dsi.lcm_ext_te_monitor = FALSE; 
#endif
		// Non-continuous clock 
		params->dsi.noncont_clock = TRUE; 
		params->dsi.noncont_clock_period = 2; // Unit : frames
		
		
		params->dsi.esd_check_enable = 1; //enable ESD check
		params->dsi.customization_esd_check_enable = 1; //0 TE ESD CHECK  1 LCD REG CHECK

		params->dsi.lcm_esd_check_table[0].cmd            = 0x0A;
		params->dsi.lcm_esd_check_table[0].count        = 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9C;
	/*		
		params->dsi.lcm_esd_check_table[1].cmd          = 0xAC;
		params->dsi.lcm_esd_check_table[1].count        = 1;
		params->dsi.lcm_esd_check_table[1].para_list[0] = 0x00;
	*/

}


static void lcm_init(void)
{
	lcd_power_en(1);
	//MDELAY(50);
	
	SET_RESET_PIN(1);
	MDELAY(10);
	SET_RESET_PIN(0);
	MDELAY(20);
	SET_RESET_PIN(1);
	MDELAY(20);

    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);

}


static void lcm_suspend(void)
{

        push_table(lcm_sleep_in_setting, sizeof(lcm_sleep_in_setting) / sizeof(struct LCM_setting_table), 1);
		
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
  	MDELAY(10);

}


static void lcm_resume(void)
{

	lcm_init();
//	push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
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


LCM_DRIVER nt35521_hd720_dsi_vdo_cmi_kyd_fpc055nnh161a1_mx2151e_sft_j5501_lcm_drv = 
{
    .name			= "nt35521_hd720_dsi_vdo_cmi_kyd_fpc055nnh161a1_mx2151e_sft_j5501",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
};
