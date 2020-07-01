/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
 
/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

#ifndef BUILD_LK
#include <linux/string.h>
#endif
#include "lcm_drv.h"
#ifdef BUILD_LK
	#include <platform/upmu_common.h>
	#include <platform/upmu_hw.h>
	#include <platform/mt_gpio.h>
	#include <platform/mt_i2c.h> 
	#include <platform/mt_pmic.h>
	#include <string.h>
#else
	#include <mt-plat/upmu_common.h>
	#include <mt-plat/mt_gpio.h>
	#include <linux/gpio.h>
	#include <linux/kernel.h>
#endif

// ---------------------------------------------------------------------------
//  Local Constants
// ---------------------------------------------------------------------------

#define FRAME_WIDTH  (720)
#define FRAME_HEIGHT (1280)

#define REGFLAG_DELAY             							0XF1
#define REGFLAG_END_OF_TABLE      							0x100   // END OF REGISTERS MARKER

//#define LCM_ID_JD9365   0x9365
#define LCM_DSI_CMD_MODE                                    0

// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------
//static unsigned int lcm_esd_test = FALSE; ///only for ESD test
//static unsigned int lcm_check_status(void);
static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))


#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))



#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)    lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)        lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)                                    lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)                lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg                                            lcm_util.dsi_read_reg()
#define read_reg_v2(cmd, buffer, buffer_size)                   lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)  

static void lcd_power_en(unsigned char enabled)
{
	if (enabled)
	{

		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x03); //for ctp 1151
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


static void lcm_set_util_funcs(const LCM_UTIL_FUNCS *util)
{
    memcpy(&lcm_util, util, sizeof(LCM_UTIL_FUNCS));
}

static struct LCM_setting_table lcm_initialization_setting[] = {

{0xE0,1,{0x00}},

//--- PASSWORD  ----//
{0xE1, 1, {0x93}},
{0xE2, 1, {0x65}},
{0xE3, 1, {0xF8}},
{0x80, 1, {0x03}},

//--- Sequence Ctrl  ----//
//{0x70, 1, {0x10}},	//DC0,DC1
//{0x71, 1, {0x13}},	//DC2,DC3
//{0x72, 1, {0x06}},	//DC7

//{0x80, 1, {0x03}},// 4lanes

//--- Page1  ----//
{0xE0, 1, {0x01}},

//Set VCOM
{0x00, 1, {0x00}},
{0x01, 1, {0x67}},//90
{0x03, 1, {0x00}},
{0x04, 1, {0x67}},//9E

//{0x0C, 1, {0x64}},

//Set Gamma Power, VGMP,VGMN,VGSP,VGSN
{0x17, 1, {0x00}},
{0x18, 1, {0xC9}},//4.6V
{0x19, 1, {0x01}},
{0x1A, 1, {0x00}},
{0x1B, 1, {0xC9}},//-4.6V
{0x1C, 1, {0x01}},

//Set Gate Power
{0x1F, 1, {0x79}},
{0x20, 1, {0x2D}},
{0x21, 1, {0x2D}},
{0x22, 1, {0x4F}},
{0x26, 1, {0xF1}},

//SET RGBCYC
{0x37, 1, {0x05}},	//REV=1,BGR=1 9->5 gl 20170715

//SET RGBCYC
{0x38, 1, {0x04}},	//JDT=101 zigzag inversion
{0x39, 1, {0x0C}},	//RGB_N_EQ1, modify 20140806
{0x3A, 1, {0x18}},	//RGB_N_EQ2, modify 20140806
{0x3C, 1, {0x78}},
//{0x3D, 1, {0xFF}},
//{0x3E, 1, {0xFF}},
//{0x3F, 1, {0x7F}},

//Set TCON
{0x40, 1, {0x04}},	//RSO=
{0x41, 1, {0xA0}},	//LN=640->1280 line
//{0x43, 1, {0x0F}},	//
//{0x44, 1, {0x0D}},	//
//{0x45, 1, {0x28}},	//


//--- power voltage  ----//
{0x55, 1, {0x01}},	//DCDCM=0001, JD PWR_IC
{0x56, 1, {0x01}},
{0x57, 1, {0x6D}},
{0x58, 1, {0x0A}},
{0x59, 1, {0x1A}},	//VCL = -2.9V
{0x5A, 1, {0x65}},	//VGH = 15.2V
{0x5B, 1, {0x14}},	//VGL = -12V
{0x5C, 1, {0x16}},


//--- Gamma  ----//   
{0x5D,1,{0x70}},
{0x5E,1,{0x4B}},
{0x5F,1,{0x36}},
{0x60,1,{0x27}},
{0x61,1,{0x1F}},
{0x62,1,{0x10}},
{0x63,1,{0x15}},
{0x64,1,{0x01}},
{0x65,1,{0x1E}},
{0x66,1,{0x21}},
{0x67,1,{0x24}},
{0x68,1,{0x44}},
{0x69,1,{0x36}},
{0x6A,1,{0x45}},
{0x6B,1,{0x38}},
{0x6C,1,{0x3B}},
{0x6D,1,{0x2F}},
{0x6E,1,{0x1F}},
{0x6F,1,{0x02}},
{0x70,1,{0x70}},
{0x71,1,{0x4B}},
{0x72,1,{0x36}},
{0x73,1,{0x27}},
{0x74,1,{0x1F}},
{0x75,1,{0x10}},
{0x76,1,{0x15}},
{0x77,1,{0x01}},
{0x78,1,{0x1E}},
{0x79,1,{0x21}},
{0x7A,1,{0x24}},
{0x7B,1,{0x44}},
{0x7C,1,{0x36}},
{0x7D,1,{0x45}},
{0x7E,1,{0x38}},
{0x7F,1,{0x3B}},
{0x80,1,{0x2F}},
{0x81,1,{0x1F}},
{0x82,1,{0x02}},

//Page2, for GIP
{0xE0, 1, {0x02}},

//GIP_L Pin mapping
{0x00,1,{0x13}},
{0x01,1,{0x11}},
{0x02,1,{0x0B}},
{0x03,1,{0x09}},
{0x04,1,{0x07}},
{0x05,1,{0x05}},
{0x06,1,{0x1F}},
{0x07,1,{0x1F}},
{0x08,1,{0x1F}},
{0x09,1,{0x1F}},
{0x0A,1,{0x1F}},
{0x0B,1,{0x1F}},
{0x0C,1,{0x1F}},
{0x0D,1,{0x1F}},
{0x0E,1,{0x1F}},
{0x0F,1,{0x1F}},
{0x10,1,{0x1F}},
{0x11,1,{0x1F}},
{0x12,1,{0x01}},
{0x13,1,{0x03}},
{0x14,1,{0x1F}},
{0x15,1,{0x1F}},
 
//GIP_R Pin mapping
{0x16,1,{0x12}},
{0x17,1,{0x10}},
{0x18,1,{0x0A}},
{0x19,1,{0x08}},
{0x1A,1,{0x06}},
{0x1B,1,{0x04}},
{0x1C,1,{0x1F}},
{0x1D,1,{0x1F}},
{0x1E,1,{0x1F}},
{0x1F,1,{0x1F}},
{0x20,1,{0x1F}},
{0x21,1,{0x1F}},
{0x22,1,{0x1F}},
{0x23,1,{0x1F}},
{0x24,1,{0x1F}},
{0x25,1,{0x1F}},
{0x26,1,{0x1F}},
{0x27,1,{0x1F}},
{0x28,1,{0x00}},
{0x29,1,{0x02}},
{0x2A,1,{0x1F}},
{0x2B,1,{0x1F}},

//GIP_L_GS Pin mapping
{0x2C,1,{0x00}},
{0x2D,1,{0x02}},
{0x2E,1,{0x08}},
{0x2F,1,{0x0A}},
{0x30,1,{0x04}},
{0x31,1,{0x06}},
{0x32,1,{0x1F}},
{0x33,1,{0x1F}},
{0x34,1,{0x1F}},
{0x35,1,{0x1F}},
{0x36,1,{0x1F}},
{0x37,1,{0x1F}},
{0x38,1,{0x1F}},
{0x39,1,{0x1F}},
{0x3A,1,{0x1F}},
{0x3B,1,{0x1F}},
{0x3C,1,{0x1F}},
{0x3D,1,{0x1F}},
{0x3E,1,{0x12}},
{0x3F,1,{0x10}},
{0x40,1,{0x1F}},
{0x41,1,{0x1F}},

//GIP_R_GS Pin mapping
{0x42,1,{0x01}},
{0x43,1,{0x03}},
{0x44,1,{0x09}},
{0x45,1,{0x0B}},
{0x46,1,{0x05}},
{0x47,1,{0x07}},
{0x48,1,{0x1F}},
{0x49,1,{0x1F}},
{0x4A,1,{0x1F}},
{0x4B,1,{0x1F}},
{0x4C,1,{0x1F}},
{0x4D,1,{0x1F}},
{0x4E,1,{0x1F}},
{0x4F,1,{0x1F}},
{0x50,1,{0x1F}},
{0x51,1,{0x1F}},
{0x52,1,{0x1F}},
{0x53,1,{0x1F}},
{0x54,1,{0x13}},
{0x55,1,{0x11}},
{0x56,1,{0x1F}},
{0x57,1,{0x1F}},

{0x58,1,{0x40}},
{0x59,1,{0x00}},
{0x5A,1,{0x00}},
{0x5B,1,{0x30}},
{0x5C,1,{0x09}},
{0x5D,1,{0x30}},
{0x5E,1,{0x01}},
{0x5F,1,{0x02}},
{0x60,1,{0x30}},
{0x61,1,{0x01}},
{0x62,1,{0x02}},
{0x63,1,{0x03}},
{0x64,1,{0x64}},
{0x65,1,{0x75}},
{0x66,1,{0x0D}},
{0x67,1,{0x72}},
{0x68,1,{0x0C}},
{0x69,1,{0x10}},
{0x6A,1,{0x64}},
{0x6B,1,{0x08}},
{0x6C,1,{0x00}},
{0x6D,1,{0x00}},
{0x6E,1,{0x00}},
{0x6F,1,{0x00}},
{0x70,1,{0x00}},
{0x71,1,{0x00}},
{0x72,1,{0x06}},
{0x73,1,{0x86}},
{0x74,1,{0x00}},
{0x75,1,{0x07}},
{0x76,1,{0x00}},
{0x77,1,{0x5D}},
{0x78,1,{0x19}},
{0x79,1,{0x00}},
{0x7A,1,{0x05}},
{0x7B,1,{0x05}},
{0x7C,1,{0x00}},
{0x7D,1,{0x03}},
{0x7E,1,{0x86}},

//Page4			
{0xE0,1,{0x04}},	
{0x09,1,{0x10}},	
{0x2B,1,{0x2B}},		
{0x2E,1,{0x44}},
{0x2D,1,{0x03}},		
		
//Page0		
{0xE0,1,{0x00}},		
{0xE6,1,{0x02}},	 	
{0xE7,1,{0x02}},	

	{0x11,1,{0x00}},                 // Sleep-Out

	{REGFLAG_DELAY,120,{}},

	{0x29,1,{0x00}},                 // Display On
	
	{REGFLAG_DELAY,5,{}},	
	{0x35, 1, {0x00}},

	{REGFLAG_DELAY,10,{}},

	//--- PASSWORD  ----//
	{0xE0,1,{0x00}},
	{0xE1,1,{0x09}},
	{0xE2,1,{0xB1}},
	{0xE3,1,{0x7F}},

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


static void lcm_get_params(LCM_PARAMS *params)
{

  	memset(params, 0, sizeof(LCM_PARAMS));	
		params->type   = LCM_TYPE_DSI;
		params->width  = FRAME_WIDTH;
		params->height = FRAME_HEIGHT;
        #if (LCM_DSI_CMD_MODE)
		params->dsi.mode   = CMD_MODE;
        #else
		params->dsi.mode   = BURST_VDO_MODE;//SYNC_PULSE_VDO_MODE BURST_VDO_MODE  SYNC_PULSE_VDO_MODE;//BURST_VDO_MODE; 
        #endif
	
		// DSI
		/* Command mode setting */
		//1 Three lane or Four lane
		params->dsi.LANE_NUM				= LCM_FOUR_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Video mode setting		
		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
		
		params->dsi.vertical_sync_active				= 4;
    		params->dsi.vertical_backporch					= 12;
		params->dsi.vertical_frontporch 				= 24;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 40;//24
		params->dsi.horizontal_backporch				= 40;//48
		params->dsi.horizontal_frontporch				= 50;//40
		params->dsi.horizontal_active_pixel 			= FRAME_WIDTH;

	  //params->dsi.LPX=8; 
		// Bit rate calculation
		//1 Every lane speed
		//params->dsi.pll_select=1;
		//params->dsi.PLL_CLOCK  = LCM_DSI_6589_PLL_CLOCK_377;
		params->dsi.PLL_CLOCK=210;//175;//208
	//add by tjf start 20160326
#if 1
		params->dsi.esd_check_enable = 1;
		params->dsi.customization_esd_check_enable = 1;
		
		params->dsi.lcm_esd_check_table[0].cmd          = 0x0a;//0a
		params->dsi.lcm_esd_check_table[0].count        = 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9c;//9c
		params->dsi.noncont_clock=1;
		params->dsi.noncont_clock_period=1;
#endif		
		//params->dsi.lcm_esd_check_table[0].cmd          = 0xac;//0a
		//params->dsi.lcm_esd_check_table[0].count        = 1;
		//params->dsi.lcm_esd_check_table[0].para_list[0] = 0x00;//9c
		

		//add by tjf end
		//params->dsi.compatibility_for_nvk = 1;// this parameter would be set to 1 if DriverIC is NTK's and when force match DSI clock for NTK's
}




static void lcm_init(void)
{
    
    lcd_power_en(1);
    MDELAY(50);
    SET_RESET_PIN(1);
    MDELAY(10);
    SET_RESET_PIN(0);
    MDELAY(20);
    SET_RESET_PIN(1);
    MDELAY(50);
    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
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
    dsi_set_cmdq(data_array, 3, 1);
    
    data_array[0]= 0x00053902;
    data_array[1]= (y1_MSB<<24)|(y0_LSB<<16)|(y0_MSB<<8)|0x2b;
    data_array[2]= (y1_LSB);
    dsi_set_cmdq(data_array, 3, 1);
    
    data_array[0]= 0x00290508;
    dsi_set_cmdq(data_array, 1, 1);
    
    data_array[0]= 0x002c3909;
    dsi_set_cmdq(data_array, 1, 0);


}
#endif
static void lcm_suspend(void)
{
    unsigned int data_array[16];
    MDELAY(20);
    data_array[0]=0x00E01500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x93E11500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x65E21500;
    dsi_set_cmdq(data_array,1,1); 
    
    data_array[0]=0xF8E31500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x00280500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(20);
    data_array[0]=0x00100500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(150);
    

    data_array[0]=0x00E01500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x09E11500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0xB1E21500;
    dsi_set_cmdq(data_array,1,1); 
    
    data_array[0]=0x7FE31500;
    dsi_set_cmdq(data_array,1,1);

    SET_RESET_PIN(0);
    MDELAY(200);

}

static void lcm_resume(void)
{
     SET_RESET_PIN(1);
    MDELAY(10);
    SET_RESET_PIN(0);
    MDELAY(20);
    SET_RESET_PIN(1);
    MDELAY(50);
    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);

  
  //lcm_init();
   /*
   
    unsigned int data_array[16];
    data_array[0]=0x00110500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(120);
    data_array[0]=0x00290500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(10);
    */
}

static unsigned int lcm_compare_id(void)
{

	unsigned int lcd_id1,lcd_id2 = 0;
	lcd_power_en(1);
	MDELAY(50);
#ifdef BUILD_LK
	lcd_id1 = mt_get_gpio_in(GPIO_LCD_ID1_PIN);
	lcd_id2 = mt_get_gpio_in(GPIO_LCD_ID2_PIN);
	printf("lcd_id1 == %d , lcd_id2 == %d.\n",lcd_id1, lcd_id2);
#else
	lcd_id1 = gpio_get_value(GPIO19_LCD_ID1_PIN);
	lcd_id2 = gpio_get_value(GPIO20_LCD_ID2_PIN);
	printk(KERN_ERR "lcd_id1 == %d , lcd_id2 == %d.\n",lcd_id1, lcd_id2);
#endif

	
	if((lcd_id1==1)&&(lcd_id2==0))
		return 1;
	else
		return 0;
}


LCM_DRIVER jd9365_hd720_dsi_vdo_ivo_songrui_050rivd94_mx2521e_yaao_x27_lcm_drv = 
{
        .name   	= "jd9365_hd720_dsi_vdo_ivo_songrui_050rivd94_mx2521e_yaao_x27",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
#if (LCM_DSI_CMD_MODE)
    .update         = lcm_update,
#endif
};
