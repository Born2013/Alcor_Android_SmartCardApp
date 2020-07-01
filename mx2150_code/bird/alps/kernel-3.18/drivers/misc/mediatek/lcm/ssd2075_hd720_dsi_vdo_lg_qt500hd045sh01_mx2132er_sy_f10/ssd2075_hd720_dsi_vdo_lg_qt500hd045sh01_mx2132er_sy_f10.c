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

#define FRAME_WIDTH  (720)
#define FRAME_HEIGHT (1280)

#define LCM_ID_SSD2075 (0x2075)

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

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))

#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))


// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	        lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)										lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)					lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg(cmd)											lcm_util.dsi_dcs_read_lcm_reg(cmd)
#define read_reg_v2(cmd, buffer, buffer_size)   				lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)   


#define   LCM_DSI_CMD_MODE							0

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

static void init_lcm_registers(void)
{
	unsigned int data_array[16];

	
	data_array[0] = 0x00023902;                          
	data_array[1] = 0x0000A3E1;                 
	dsi_set_cmdq(data_array, 2, 1);
	//MDELAY(1);

	data_array[0] = 0x00023902;                          
	data_array[1] = 0x000000B3;                 
	dsi_set_cmdq(data_array, 2, 1);
	//MDELAY(1);


	data_array[0] = 0x00053902;                          
	data_array[1] = 0x000F16B6; 
	data_array[2] = 0x00000077; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00093902;                          
	data_array[1] = 0x080600B8; 
	data_array[2] = 0x23090700; 
	data_array[3] = 0x00000004; 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);

	data_array[0] = 0x00073902;                          
	data_array[1] = 0x220804B9; 
	data_array[2] = 0x000FFFFF;  //0x00FFFFFF
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00093902;                          
	data_array[1] = 0x100E0EBA; 
	data_array[2] = 0x0C0A0A10; 
	data_array[3] = 0x0000000C; 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);

	data_array[0] = 0x00093902;                          
	data_array[1] = 0xA1A1A1BB; 
	data_array[2] = 0xA1A1A1A1; 
	data_array[3] = 0x000000A1; 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);


	data_array[0] = 0x00093902;                          
	data_array[1] = 0x000000BC; 
	data_array[2] = 0x00000000; 
	data_array[3] = 0x00000000; 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);

	data_array[0] = 0x00093902;                          
	data_array[1] = 0x110F0FBD; 
	data_array[2] = 0x0D0B0B11; 
	data_array[3] = 0x0000000D; 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);


	data_array[0] = 0x00093902;                          
	data_array[1] = 0xA1A1A1BE; 
	data_array[2] = 0xA1A1A1A1; 
	data_array[3] = 0x000000A1; 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);


	data_array[0] = 0x00093902;                          
	data_array[1] = 0x000000BF; 
	data_array[2] = 0x00000000; 
	data_array[3] = 0x00000000; 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);


	data_array[0] = 0x00043902;                          
	data_array[1] = 0x121E16B1;  //modified  14+3             
	dsi_set_cmdq(data_array, 2, 1); 
	//MDELAY(1);


	data_array[0] = 0x00063902;                          
	data_array[1] = 0x020301E0; //VSA
	data_array[2] = 0x00000100; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);


	data_array[0] = 0x00073902;                          
	data_array[1] = 0x050000D0; 
	data_array[2] = 0x00120704; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);


	data_array[0] = 0x00063902;                          
	data_array[1] = 0x2A3F0DD1; 
	data_array[2] = 0x00000509; 
	dsi_set_cmdq(data_array, 3, 1);
	//MDELAY(1);


	data_array[0] = 0x00073902;                          
	data_array[1] = 0x050000D2; 
	data_array[2] = 0x00120704; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);


	data_array[0] = 0x00063902;                          
	data_array[1] = 0x2A3F0DD3; 
	data_array[2] = 0x00000509; 
	dsi_set_cmdq(data_array, 3, 1);
	//MDELAY(1);


	data_array[0] = 0x00073902;                          
	data_array[1] = 0x050000D4; 
	data_array[2] = 0x00120704; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);


	data_array[0] = 0x00063902;                          
	data_array[1] = 0x2A3F0DD5; 
	data_array[2] = 0x00000509; 
	dsi_set_cmdq(data_array, 3, 1);  
	//MDELAY(1);

	data_array[0] = 0x00073902;                          
	data_array[1] = 0x050000D6; 
	data_array[2] = 0x00120704; 
	dsi_set_cmdq(data_array, 3, 1);
	//MDELAY(1);

	data_array[0] = 0x00063902;                          
	data_array[1] = 0x2A3F0DD7; 
	data_array[2] = 0x00000509; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00073902;                          
	data_array[1] = 0x050000D8; 
	data_array[2] = 0x00120704; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00063902;                          
	data_array[1] = 0x2A3F0DD9; 
	data_array[2] = 0x00000509; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00073902;                          
	data_array[1] = 0x050000DA; 
	data_array[2] = 0x00120704;  
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00063902;                          
	data_array[1] = 0x2A3F0DDB; 
	data_array[2] = 0x00000509; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00053902;                          
	data_array[1] = 0xFF00D870; 
	data_array[2] = 0x00000080; 
	dsi_set_cmdq(data_array, 3, 1); 
	//MDELAY(1);

	data_array[0] = 0x00023902;                          
	data_array[1] = 0x000001FF;                 
	dsi_set_cmdq(data_array, 2, 1); 
	MDELAY(1);

	// add cmd-c6 
	data_array[0] = 0x00033902; 						 
	data_array[1] = 0x003399C6; 				
	dsi_set_cmdq(data_array, 2, 1); 
	MDELAY(1);
	//add end

	//legen modify 
	data_array[0] = 0x00033902;                          
	data_array[1] = 0x00309DDE; //00309DDE
	//data_array[2] = 0x00130D0C; 
	dsi_set_cmdq(data_array, 2, 1);


	data_array[0] = 0x00023902;                          
	data_array[1] = 0x00000014; 
	//data_array[2] = 0x00130D0C; 
	dsi_set_cmdq(data_array, 2, 1);
	//legen modify end


	data_array[0] = 0x00023902;                          
	data_array[1] = 0x000007E9;                 
	dsi_set_cmdq(data_array, 2, 1); 
	MDELAY(1);

	data_array[0] = 0x00033902;                          
	data_array[1] = 0x001060ED;                 
	dsi_set_cmdq(data_array, 2, 1); 
	//MDELAY(1);

	data_array[0] = 0x00023902;                          
	data_array[1] = 0x000012EC;                 
	dsi_set_cmdq(data_array, 2, 1); 
	//MDELAY(1);

	data_array[0] = 0x00053902;                          
	data_array[1] = 0x347B77CD; 
	data_array[2] = 0x00000008; 
	dsi_set_cmdq(data_array, 3, 1);
	//MDELAY(1);

	data_array[0] = 0x00083902;                          
	data_array[1] = 0x340503C3;     //   0x350F03C3
	data_array[2] = 0x54440105; 
	dsi_set_cmdq(data_array, 3, 1);
	//MDELAY(1);

	data_array[0] = 0x00063902; 
	data_array[1] = 0x450302C4;  //0x701303c4 //0x700302C4
	data_array[2] = 0x00006548;  //0x00005C70
	dsi_set_cmdq(data_array, 3, 1);
	//MDELAY(1);


	data_array[0] = 0x00043902;                          
	data_array[1] = 0x0080DFCB;   //0x0080DACB     //line<20130109>wangyanhui           
	dsi_set_cmdq(data_array, 2, 1); 
	//MDELAY(1);

	data_array[0] = 0x00033902;                          
	data_array[1] = 0x002815EA;                 
	dsi_set_cmdq(data_array, 2, 1); 
	//MDELAY(1);

	data_array[0] = 0x00053902;                          
	data_array[1] = 0x000038F0; 
	data_array[2] = 0x00000000; 
	dsi_set_cmdq(data_array, 3, 1);
	//MDELAY(1);

	data_array[0] = 0x00043902;                          
	data_array[1] = 0x820060C9;                 
	dsi_set_cmdq(data_array, 2, 1);
	//MDELAY(1);

	data_array[0] = 0x00093902;                          
	data_array[1] = 0x050500B5;
	data_array[2] = 0x2040041E;                          
	data_array[3] = 0x000000FC;                 
	dsi_set_cmdq(data_array, 4, 1);
	//MDELAY(1);

	data_array[0] = 0x00023902;                          
	data_array[1] = 0x00000836;                 
	dsi_set_cmdq(data_array, 2, 1);
	MDELAY(1);//wait for PLL to lock 

	//1 Do not delete 0x11, 0x29 here
	data_array[0] = 0x00110500; // Sleep Out
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(120);

	data_array[0] = 0x00290500; // Display On
	dsi_set_cmdq(data_array, 1, 1); 
	data_array[0] = 0x00053902;                          
	data_array[1] = 0xFFFF18F0;  
	data_array[2] = 0x00000000;  	
	dsi_set_cmdq(data_array, 3, 1);
	
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

       //1 SSD2075 has no TE Pin
		// enable tearing-free
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_DISABLED;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

        #if (LCM_DSI_CMD_MODE)
		params->dsi.mode   = CMD_MODE;
        #else
		//params->dsi.mode   = SYNC_PULSE_VDO_MODE;
		params->dsi.mode   = BURST_VDO_MODE;
		//params->dsi.mode   = SYNC_EVENT_VDO_MODE; 
		
        #endif
	
		// DSI
		/* Command mode setting */
		//1 Three lane or Four lane
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
		params->dsi.intermediat_buffer_num = 0;//because DSI/DPI HW design change, this parameters should be 0 when video mode in MT658X; or memory leakage

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
		params->dsi.word_count=720*3;	

		
		params->dsi.vertical_sync_active				= 3;  //---3
		params->dsi.vertical_backporch					= 12 ; //---14   12	zhuxiankun 12
		params->dsi.vertical_frontporch					= 8;  //----8
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 2;  //----2
		params->dsi.horizontal_backporch				= 28; //----28
		params->dsi.horizontal_frontporch				= 50; //----50
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

		params->dsi.PLL_CLOCK=220;

		params->dsi.HS_PRPR=4;//3//
		params->dsi.CLK_HS_POST = 22;
		params->dsi.DA_HS_EXIT =35;
	    	params->dsi.LPX=13; 
		//params->dsi.HS_PRPR=5;
		//params->dsi.HS_TRAIL=13;

		// Bit rate calculation
		//1 Every lane speed
		//params->dsi.pll_div1=0;		// div1=0,1,2,3;div1_real=1,2,4,4 ----0: 546Mbps  1:273Mbps
		//params->dsi.pll_div2=1;		// div2=0,1,2,3;div1_real=1,2,4,4	
		//params->dsi.fbk_div =16;    // fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)	

}

static void lcm_init(void)
{
	lcd_power_en(1);
	MDELAY(50);
	
	SET_RESET_PIN(1);
	MDELAY(5);    //>1ms
	SET_RESET_PIN(0);
	MDELAY(40);  // >30ms
	
	SET_RESET_PIN(1);
	MDELAY(80);    // >60ms      

	init_lcm_registers();

}



static void lcm_suspend(void)
{
	unsigned int data_array[16];
#if 1
	data_array[0] = 0x00280500; // Sleep Out
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(20);
	
	data_array[0] = 0x00100500; // Display On
	dsi_set_cmdq(data_array, 1, 1); 
	MDELAY(120);
#endif

#if 1

	data_array[0] = 0x00023902;                          
	data_array[1] = 0x000001FF;                 
	dsi_set_cmdq(data_array, 2, 1);
	
	data_array[0] = 0x00073902;                          
	data_array[1] = 0x111815DE;
	data_array[2] = 0x00180F10;   
	dsi_set_cmdq(data_array, 3, 1);

	data_array[0] = 0x00083902;                          
	data_array[1] = 0x340600C3;
	data_array[2] = 0x54440105;   
	dsi_set_cmdq(data_array, 3, 1); 

	data_array[0] = 0x00033902;                          
	data_array[1] = 0x00100CCE; 
	dsi_set_cmdq(data_array, 2, 1);
#endif


	//SET_RESET_PIN(1);
	SET_RESET_PIN(0);
	MDELAY(1);
	//SET_RESET_PIN(1);
}


static void lcm_resume(void)
{
#if !defined(BUILD_LK)
	//1 do lcm init again to solve some display issue
	lcm_init();
#endif
}
         
#if (LCM_DSI_CMD_MODE)
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
	dsi_set_cmdq(&data_array, 3, 1);
	
	data_array[0]= 0x00053902;
	data_array[1]= (y1_MSB<<24)|(y0_LSB<<16)|(y0_MSB<<8)|0x2b;
	data_array[2]= (y1_LSB);
	dsi_set_cmdq(&data_array, 3, 1);

	data_array[0]= 0x00290508; //HW bug, so need send one HS packet
	dsi_set_cmdq(&data_array, 1, 1);
	
	data_array[0]= 0x002c3909;
	dsi_set_cmdq(&data_array, 1, 0);

}
#endif

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

#if 0
static unsigned int lcm_esd_check(void)
{

    #ifndef BUILD_LK
      char  buffer[4] = {0xff, 0xff, 0xff , 0xff};
      int   array[4];

     // printk(" lcm_esd_check  ");

      if(lcm_esd_test)
       {
           lcm_esd_test = FALSE;
           return TRUE;
        }

        array[0] = 0x00083700;// read id return two byte,version and id
        dsi_set_cmdq(array, 1, 1);

        read_reg_v2(0xF5, buffer, 3);
        printk(" lcm_esd_check   buffer[0] = %d  ,buffer[1] = %d,buffer[2] = %d \n",buffer[0],buffer[1],buffer[2]);

        //if(((buffer[0]&0xff)==0) && ((buffer[1]&0xf0) ==0)&&((buffer[2]&0x0f)== 0))
	if(((buffer[0]&0xff)==0) && ((buffer[1]&0xf0) ==0)&&((buffer[2]&0x01)== 0))
        {
                return FALSE;
        }
        else
        {
                return TRUE;
        }
     #endif
	 
}

static unsigned int lcm_esd_recover(void)
{
	//lcm_init();
	lcm_resume();

	return TRUE;
}
#endif

LCM_DRIVER ssd2075_hd720_dsi_vdo_lg_qt500hd045sh01_mx2132er_sy_f10_lcm_drv = 
{
    .name			= "ssd2075_hd720_dsi_vdo_lg_qt500hd045sh01_mx2132er_sy_f10",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
//	.esd_check = lcm_esd_check,
//	.esd_recover = lcm_esd_recover,
    #if (LCM_DSI_CMD_MODE)
    .update         = lcm_update,
    #endif
    };
