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
//BV050HDE-400_NT35521S_KYD_20160727 GAN
{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},                                                              
{0xFF,4,{0xAA,0x55,0xA5,0x80}},                                                                                                                                          
{0x6F,2,{0x11,0x00}},                                                                       
{0xF7,2,{0x20,0x00}},                                                                       
{0x6F,1,{0x06}},                                                                          
{0xF7,1,{0xA0}},                                                                         
{0x6F,1,{0x19}},                                                                          
{0xF7,1,{0x12}},                                                                          
{0x6F,1,{0x02}},                                                                          
{0xF7,1,{0x47}},                                                                                                                                                     
{0xBD,5,{0x01,0xA0,0x0C,0x08,0x01}},                                                              
{0x6F,1,{0x02}},                                                                        
{0xB8,1,{0x08}},                                                                        
{0xBB,2,{0x74,0x44}},                                                                       
{0xBC,2,{0x00,0x00}},                                                                       
{0xB1,2,{0x60,0x25}},                                                                      
{0xB6,1,{0x03}},                                                                          
 
{0xD1,16,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
{0xD7,12,{0x80,0x00,0x00,0x00,0x00,0x00,0x1F,0x1F,0x1F,0x1F,0x00,0x00}},
{0xD8,13,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},                                                                                                              
{0xF0,5,{0x55,0xAA,0x52,0x08,0x01}},                                                                                                                                    
{0xB0,2,{0x09,0x09}},                                                                       
{0xB1,2,{0x09,0x09}},	                                                                      
{0xBC,2,{0x98,0x00}},                                                                       
{0xBD,2,{0x98,0x00}},	                                                                      
{0xCA,1,{0x00}},                                                                         
{0xC0,1,{0x0C}},                                                                         
{0xB5,2,{0x03,0x03}}, 
{0xB3,2,{0x19,0x19}},                                                                       
{0xB4,2,{0x19,0x19}},                                                                       
                                                                       
{0xB6,2,{0x04,0x04}},                                                                       
{0xB7,2,{0x04,0x04}},                                                                       
{0xB9,2,{0x26,0x26}},                                                                       
{0xBA,2,{0x24,0x24}},                                                                       
                                  
{0xF0,5,{0x55,0xAA,0x52,0x08,0x02}},                                                                                                                  
{0x6F,1,{0x17}},                                                                          
{0xF4,1,{0x70}},                                                                          
{0x6F,1,{0x11}},                                                                          
{0xF3,1,{0x01}},                                                                          
{0x6F,1,{0x01}},                                                                          
{0xF9,1,{0x46}},                                                                          
                                                                        
{0xF0,5,{0x55,0xAA,0x52,0x08,0x06}},                                                              
                                                                     
{0xB0,2,{0x10,0x12}},                                                                       
{0xB1,2,{0x14,0x16}},		                                                              
{0xB2,2,{0x00,0x02}},                                                                       
{0xB3,2,{0x31,0x31}},	                                                                      
{0xB4,2,{0x31,0x34}},                                                                     
{0xB5,2,{0x34,0x34}},                                                                   
{0xB6,2,{0x34,0x31}},                                                                       
{0xB7,2,{0x31,0x31}},                                                                       
{0xB8,2,{0x31,0x31}},                                                                       
{0xB9,2,{0x2D,0x2E}},                                                                       
{0xBA,2,{0x2E,0x2D}},                                                                       
{0xBB,2,{0x31,0x31}},                                                                       
{0xBC,2,{0x31,0x31}},                                                                       
{0xBD,2,{0x31,0x34}},                                                                       
{0xBE,2,{0x34,0x34}},                                                                       
{0xBF,2,{0x34,0x31}},                                                                       
{0xC0,2,{0x31,0x31}},                                                                       
{0xC1,2,{0x03,0x01}},                                                                       
{0xC2,2,{0x17,0x15}},                                                                       
{0xC3,2,{0x13,0x11}},                                                                       
{0xE5,2,{0x31,0x31}},                                                                       
{0xC4,2,{0x17,0x15}},                                                                       
{0xC5,2,{0x13,0x11}},                                                                       
{0xC6,2,{0x03,0x01}},                                                                       
{0xC7,2,{0x31,0x31}},                                                                       
{0xC8,2,{0x31,0x34}},                                                                       
{0xC9,2,{0x34,0x34}},                                                                       
{0xCA,2,{0x34,0x31}},                                                                       
{0xCB,2,{0x31,0x31}},                                                                       
{0xCC,2,{0x31,0x31}},                                                                       
{0xCD,2,{0x2E,0x2D}},                                                                       
{0xCE,2,{0x2D,0x2E}},                                                                       
{0xCF,2,{0x31,0x31}},                                                                       
{0xD0,2,{0x31,0x31}},                                                                       
{0xD1,2,{0x31,0x34}},                                                                       
{0xD2,2,{0x34,0x34}},                                                                       
{0xD3,2,{0x34,0x31}},                                                                       
{0xD4,2,{0x31,0x31}},                                                                       
{0xD5,2,{0x00,0x02}},                                                                       
{0xD6,2,{0x10,0x12}},                                                                       
{0xD7,2,{0x14,0x16}},                                                                       
{0xE6,2,{0x32,0x32}},                                                                       
{0xD8,5,{0x00,0x00,0x00,0x00,0x00}},                                                              
{0xD9,5,{0x00,0x00,0x00,0x00,0x00}},                                                              
{0xE7,1,{0x00}},                                                                          
                                                                           
{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},                                                              
                                                                           
{0xED,1,{0x30}},                                                                          
                                 
                                                                          
{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},                                                              
                                                                          
{0xB0,2,{0x20,0x00}},                                                                       
{0xB1,2,{0x20,0x00}},                                                                       
                  
                                                              
{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},                                                              
                                                                
{0xB0,2,{0x17,0x06}},                                                                       
{0xB8,1,{0x00}},                                                                          
{0xBD,5,{0x03,0x03,0x00,0x00,0x03}},                                                              
{0xB1,2,{0x17,0x06}},                                                                       
{0xB9,2,{0x00,0x03}},                                                                       
{0xB2,2,{0x17,0x06}},                                                                       
{0xBA,2,{0x00,0x00}},                                                                       
{0xB3,2,{0x17,0x06}},                                                                       
{0xBB,2,{0x00,0x00}},                                                                       
{0xB4,2,{0x17,0x06}},                                                                       
{0xB5,2,{0x17,0x06}},                                                                       
{0xB6,2,{0x17,0x06}},                                                                       
{0xB7,2,{0x17,0x06}},                                                                       
{0xBC,2,{0x00,0x03}},                                                                       
{0xE5,1,{0x06}},                                                                         
{0xE6,1,{0x06}},                                                                         
{0xE7,1,{0x06}},                                                                         
{0xE8,1,{0x06}},                                                                         
{0xE9,1,{0x06}},                                                                         
{0xEA,1,{0x06}},                                                                         
{0xEB,1,{0x06}},                                                                         
{0xEC,1,{0x06}},                                                                         
                                                                         
{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},                                                              
                                                                        
{0xC0,1,{0x0D}},                                                                         
{0xC1,1,{0x0B}},                                                                         
{0xC2,1,{0x23}},                                                                         
{0xC3,1,{0x40}},                                                                         
                                                                     
{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},                                                              
                                                               
{0xB2,5,{0x05,0x00,0x0A,0x00,0x00}},                                                              
{0xB3,5,{0x05,0x00,0x0A,0x00,0x00}},                                                              
{0xB4,5,{0x05,0x00,0x0A,0x00,0x00}},                                                              
{0xB5,5,{0x05,0x00,0x0A,0x00,0x00}},                                                              
                                                             
{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},                                                              
                                                                           
{0xC4,1,{0x84}},                                                                          
{0xC5,1,{0x82}},                                                                          
{0xC6,1,{0x82}},                                                                          
{0xC7,1,{0x80}},                                                                          
                                                                           
{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},                                                              
                                                
{0xB6,5,{0x02,0x00,0x0A,0x00,0x00}},                                                              
{0xB7,5,{0x02,0x00,0x0A,0x00,0x00}},                                                              
{0xB8,5,{0x02,0x00,0x0A,0x00,0x00}},                                                              
{0xB9,5,{0x02,0x00,0x0A,0x00,0x00}},                                                              
                                                               
{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},                                                              
                                                                          
{0xC8,2,{0x0B,0x30}},                                                                       
{0xC9,2,{0x05,0x10}},                                                                       
{0xCA,2,{0x01,0x10}},                                                                       
{0xCB,2,{0x01,0x10}},                                                                       
                                                                       
{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},	                                                      
                                               
{0xBA,5,{0x53,0x00,0x0A,0x00,0x00}},                                                              
{0xBB,5,{0x53,0x00,0x0A,0x00,0x00}},                                                              
{0xBC,5,{0x53,0x00,0x0A,0x00,0x00}},                                                              
{0xBD,5,{0x53,0x00,0x0A,0x00,0x00}},                                                              
                                                              
{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},                                                              
                                                              
{0xD1,5,{0x03,0x05,0x05,0x07,0x00}},                                                              
{0xD2,5,{0x03,0x05,0x09,0x03,0x00}},                                                              
{0xD3,5,{0x00,0x00,0x6A,0x07,0x10}},                                                              
{0xD4,5,{0x30,0x00,0x6A,0x07,0x10}},                                                              
                                                              
{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},                                                              
                                                                          
{0xC4,1,{0x60}},                                                                          
{0xC5,1,{0x40}},                                                                          
{0xC6,1,{0x64}},                                                                          
{0xC7,1,{0x44}},                                                                          
                                                                          
{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},                                                              
                                              
{0xBD,5,{0x01,0xA0,0x10,0x10,0x01}},                                                              
                                                               
{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},                                                              
                                                                 
{0xB6,1,{0x03}},                                                                   
{0xB8,4,{0x00,0x00,0x00,0x00}},                                                                 
                                                                    
{0xF0,5,{0x55,0xAA,0x52,0x08,0x01}},                                                              
                                                                           
{0xBE,1,{0x3B}},                                                                  
                                                                         
{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},                                                              
                                                                       
{0xB0,2,{0x88,0x01}},                                                                       
{0xB1,2,{0x00,0x00}},                                                                       
                                                                          
{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},                                                              
                                                            
{0xBB,2,{0x11,0x11}},                                                                
{0xB8,4,{0x01,0x02,0x0C,0x02}},                                                                 
                                                              
{0xF0,5,{0x55,0xAA,0x52,0x08,0x02}},                                                              
                                                            
{0xEE,1,{0x01}},	                                                            
                                                            
{0xB0,16,{0x00,0x00,0x00,0x1F,0x00,0x46,0x00,0x62,0x00,0x77,0x00,0x9E,0x00,0xBD,0x00,0xF3}},		
{0xB1,16,{0x01,0x19,0x01,0x56,0x01,0x86,0x01,0xD1,0x02,0x0D,0x02,0x0F,0x02,0x47,0x02,0x85}},		
{0xB2,16,{0x02,0xAF,0x02,0xE4,0x03,0x0A,0x03,0x38,0x03,0x58,0x03,0x83,0x03,0xA1,0x03,0xD1}},		
{0xB3,4,{0x03,0xFB,0x03,0xFF}},		
                                                              
{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},                                                           
{0xB1,2,{0x68,0x21}}, 
                                                                         
{0xC8,1,{0x80}},                                                                          
                                                                         
{0x6F,1,{0x06}},                                                                          
{0xF7,1,{0xA0}},                                                                          
                                                                           
{0x6F,1,{0x19}},                                                                          
{0xF7,1,{0x12}},  
                                                                                                                       
{0x11,1,{0x00}}, 
{REGFLAG_DELAY, 120, {}},
{0x29,1,{0x00}}, 
//{0xF0,0x55,0xAA,0x52,0x08,0x00
//{0xEF,0x01
//{0xEE,0x87,0x78,0x02,0x40                                                                                                                            
                                                                                                                       

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
#endif
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

	return (lcd_id  == 1)?1:0;
#else
	return 1;
#endif
}


LCM_DRIVER nt35521_hd720_dsi_vdo_kyd_boe_for_mx2109e_sop_p2_lcm_drv = 
{
    .name			= "nt35521_hd720_dsi_vdo_kyd_boe_for_mx2109e_sop_p2",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
};
