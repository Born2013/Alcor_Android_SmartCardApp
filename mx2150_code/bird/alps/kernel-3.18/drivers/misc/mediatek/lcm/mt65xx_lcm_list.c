/*
 * Copyright (C) 2015 MediaTek Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

#include "mt65xx_lcm_list.h"
#include <lcm_drv.h>
#ifdef BUILD_LK
#include <platform/disp_drv_platform.h>
#else
#include <linux/delay.h>
/* #include <mach/mt_gpio.h> */
#endif
LCM_DSI_MODE_CON lcm_dsi_mode;

/* used to identify float ID PIN status */
#define LCD_HW_ID_STATUS_LOW      0
#define LCD_HW_ID_STATUS_HIGH     1
#define LCD_HW_ID_STATUS_FLOAT 0x02
#define LCD_HW_ID_STATUS_ERROR  0x03

#ifdef BUILD_LK
#define LCD_DEBUG(fmt)  dprintf(CRITICAL, fmt)
#else
#define LCD_DEBUG(fmt, args...)  pr_debug("[KERNEL/LCM]"fmt, ##args)
#endif

LCM_DRIVER *lcm_driver_list[] = {
#if defined(OTM1284A_HD720_DSI_VDO_TM)
	&otm1284a_hd720_dsi_vdo_tm_lcm_drv,
#endif
#if defined(OTM1285A_HD720_DSI_VDO_TM)
	&otm1285a_hd720_dsi_vdo_tm_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_8163)
	&nt35595_fhd_dsi_cmd_truly_8163_lcm_drv,
#endif

#if defined(NT35523_WXGA_DSI_VDO_8163)
	&nt35523_wxga_dsi_vdo_8163_lcm_drv,
#endif

#if defined(FT8707_FHD_DSI_VDO_LGD)
	&ft8707_fhd_dsi_vdo_lgd_drv,
#endif

#if defined(EK79007_WSVGALNL_DSI_VDO)
	&ek79007_wsvgalnl_dsi_vdo_lcm_drv,
#endif

#if defined(S6E3FA2_FHD1080_DSI_VDO)
	&s6e3fa2_fhd1080_dsi_vdo_lcm_drv,
#endif

#if defined(OTM1283A_HD720_DSI_VDO_TM)
	&otm1283a_hd720_dsi_vdo_tm_lcm_drv,
#endif

#if defined(IT6151_LP079QX1_EDP_DSI_VIDEO)
	&it6151_lp079qx1_edp_dsi_video_lcm_drv,
#endif

#if defined(VVX10F008B00_WUXGA_DSI_VDO)
	&vvx10f008b00_wuxga_dsi_vdo_lcm_drv,
#endif

#if defined(KR101IA2S_DSI_VDO)
	&kr101ia2s_dsi_vdo_lcm_drv,
#endif

#if defined(KR070IA4T_DSI_VDO)
	&kr070ia4t_dsi_vdo_lcm_drv,
#endif

#if defined(HX8394A_HD720_DSI_VDO_TIANMA_V2)
	&hx8394a_hd720_dsi_vdo_tianma_v2_lcm_drv,
#endif

#if defined(OTM1283A)
	&otm1283a_6589_hd_dsi,
#endif
#if defined(OTM1282A_HD720_DSI_VDO_60HZ)
	&otm1282a_hd720_dsi_vdo_60hz_lcm_drv,
#endif
#if defined(OTM8018B_DSI_VDO_TXD_FWVGA)
	&otm8018b_dsi_vdo_txd_fwvga_lcm_drv,
#endif

#if defined(TF070MC_RGB_V18_MT6571)
	&tf070mc_rgb_v18_mt6571_lcm_drv,
#endif

#if defined(ZS070IH5015B3H6_RGB_MT6571)
	&zs070ih5015b3h6_mt6571_lcm_drv,
#endif

#if defined(OTM1282A_HD720_DSI_VDO)
	&otm1282a_hd720_dsi_vdo_lcm_drv,
#endif

#if defined(R63311_FHD_DSI_VDO)
	&r63311_fhd_dsi_vedio_lcm_drv,
#endif

#if defined(R63315_FHD_DSI_VDO_TRULY)
	&r63315_fhd_dsi_vdo_truly_lcm_drv,
#endif

#if defined(NT35517_QHD_DSI_VDO)
	&nt35517_dsi_vdo_lcm_drv,
#endif

#if defined(ILI9806E_DSI_VDO_FWVGA)
	&ili9806e_dsi_vdo_fwvga_drv,
#endif

#if defined(LP079X01)
	&lp079x01_lcm_drv,
#endif

#if defined(HX8369)
	&hx8369_lcm_drv,
#endif

#if defined(HX8369_6575)
	&hx8369_6575_lcm_drv,
#endif

#if defined(BM8578)
	&bm8578_lcm_drv,
#endif

#if defined(NT35582_MCU)
	&nt35582_mcu_lcm_drv,
#endif

#if defined(NT35582_MCU_6575)
	&nt35582_mcu_6575_lcm_drv,
#endif

#if defined(NT35590_HD720_DSI_CMD_TRULY2)
	&nt35590_hd720_dsi_cmd_truly2_lcm_drv,
#endif

#if defined(NT35590_HD720_DSI_VDO_TRULY)
	&nt35590_hd720_dsi_vdo_truly_lcm_drv,
#endif

#if defined(SSD2075_HD720_DSI_VDO_TRULY)
	&ssd2075_hd720_dsi_vdo_truly_lcm_drv,
#endif

#if defined(NT35590_HD720_DSI_CMD)
	&nt35590_hd720_dsi_cmd_drv,
#endif

#if defined(NT35590_HD720_DSI_CMD_AUO)
	&nt35590_hd720_dsi_cmd_auo_lcm_drv,
#endif

#if defined(NT35590_HD720_DSI_CMD_AUO_WVGA)
	&nt35590_hd720_dsi_cmd_auo_wvga_lcm_drv,
#endif

#if defined(NT35590_HD720_DSI_CMD_AUO_QHD)
	&nt35590_hd720_dsi_cmd_auo_qhd_lcm_drv,
#endif

#if defined(NT35590_HD720_DSI_CMD_AUO_FWVGA)
	&nt35590_hd720_dsi_cmd_auo_fwvga_lcm_drv,
#endif

#if defined(NT35590_HD720_DSI_CMD_CMI)
	&nt35590_hd720_dsi_cmd_cmi_lcm_drv,
#endif

#if defined(NT35582_RGB_6575)
	&nt35582_rgb_6575_lcm_drv,
#endif

#if  defined(NT51012_HD720_DSI_VDO)
	&nt51012_hd720_dsi_vdo_lcm_drv,
#endif

#if defined(HX8369_RGB_6585_FPGA)
	&hx8369_rgb_6585_fpga_lcm_drv,
#endif

#if defined(HX8369_RGB_6572_FPGA)
	&hx8369_rgb_6572_fpga_lcm_drv,
#endif

#if defined(HX8369_MCU_6572)
	&hx8369_mcu_6572_lcm_drv,
#endif

#if defined(HX8369A_WVGA_DSI_CMD)
	&hx8369a_wvga_dsi_cmd_drv,
#endif

#if defined(HX8369A_WVGA_DSI_VDO)
	&hx8369a_wvga_dsi_vdo_drv,
#endif

#if defined(HX8357B)
	&hx8357b_lcm_drv,
#endif

#if defined(HX8357C_HVGA_DSI_CMD)
	&hx8357c_hvga_dsi_cmd_drv,
#endif

#if defined(R61408)
	&r61408_lcm_drv,
#endif

#if defined(R61408_WVGA_DSI_CMD)
	&r61408_wvga_dsi_cmd_drv,
#endif

#if defined(HX8369_DSI_VDO)
	&hx8369_dsi_vdo_lcm_drv,
#endif

#if defined(HX8369_DSI)
	&hx8369_dsi_lcm_drv,
#endif

#if defined(HX8369_6575_DSI)
	&hx8369_dsi_6575_lcm_drv,
#endif

#if defined(HX8369_6575_DSI_NFC_ZTE)
	&hx8369_dsi_6575_lcm_drv,
#endif

#if defined(HX8369_6575_DSI_HVGA)
	&hx8369_dsi_6575_hvga_lcm_drv,
#endif

#if defined(HX8369_6575_DSI_QVGA)
	&hx8369_dsi_6575_qvga_lcm_drv,
#endif

#if defined(HX8369_HVGA)
	&hx8369_hvga_lcm_drv,
#endif

#if defined(NT35510)
	&nt35510_lcm_drv,
#endif

#if defined(NT35510_RGB_6575)
	&nt35510_dpi_lcm_drv,
#endif

#if defined(NT35510_HVGA)
	&nt35510_hvga_lcm_drv,
#endif

#if defined(NT35510_QVGA)
	&nt35510_qvga_lcm_drv,
#endif

#if defined(NT35510_WVGA_DSI_CMD)
	&nt35510_wvga_dsi_cmd_drv,
#endif

#if defined(NT35510_6517)
	&nt35510_6517_lcm_drv,
#endif

#if defined(NT35510_DSI_CMD_6572)
	&nt35510_dsi_cmd_6572_drv,
#endif

#if defined(NT35510_DSI_CMD_6572_HVGA)
	&nt35510_dsi_cmd_6572_hvga_drv,
#endif

#if defined(NT35510_DSI_CMD_6572_FWVGA)
	&nt35510_dsi_cmd_6572_fwvga_drv,
#endif

#if defined(NT35510_DSI_CMD_6572_QVGA)
	&nt35510_dsi_cmd_6572_qvga_drv,
#endif

#if defined(NT35510_DSI_VDO_6572)
	&nt35510_dsi_vdo_6572_drv,
#endif

#if defined(NT35510_DPI_6572)
	&nt35510_dpi_6572_lcm_drv,
#endif

#if defined(NT35510_MCU_6572)
	&nt35510_mcu_6572_lcm_drv,
#endif

#if defined(ILI9481)
	&ili9481_lcm_drv,
#endif

#if defined(NT35582)
	&nt35582_lcm_drv,
#endif

#if defined(S6D0170)
	&s6d0170_lcm_drv,
#endif

#if defined(SPFD5461A)
	&spfd5461a_lcm_drv,
#endif

#if defined(TA7601)
	&ta7601_lcm_drv,
#endif

#if defined(TFT1P3037)
	&tft1p3037_lcm_drv,
#endif

#if defined(HA5266)
	&ha5266_lcm_drv,
#endif

#if defined(HSD070IDW1)
	&hsd070idw1_lcm_drv,
#endif

#if defined(HX8363_6575_DSI)
	&hx8363_6575_dsi_lcm_drv,
#endif

#if defined(HX8363_6575_DSI_HVGA)
	&hx8363_6575_dsi_hvga_lcm_drv,
#endif

#if defined(HX8363B_WVGA_DSI_CMD)
	&hx8363b_wvga_dsi_cmd_drv,
#endif

#if defined(LG4571)
	&lg4571_lcm_drv,
#endif

#if defined(LG4573B_WVGA_DSI_VDO_LH430MV1)
	&lg4573b_wvga_dsi_vdo_lh430mv1_drv,
#endif

#if defined(LVDS_WSVGA)
	&lvds_wsvga_lcm_drv,
#endif

#if defined(LVDS_WSVGA_TI)
	&lvds_wsvga_ti_lcm_drv,
#endif

#if defined(LVDS_WSVGA_TI_N)
	&lvds_wsvga_ti_n_lcm_drv,
#endif

#if defined(NT35565_3D)
	&nt35565_3d_lcm_drv,
#endif

#if defined(TM070DDH03)
	&tm070ddh03_lcm_drv,
#endif
#if defined(R63303_IDISPLAY)
	&r63303_idisplay_lcm_drv,
#endif

#if defined(HX8369B_DSI_VDO)
	&hx8369b_dsi_vdo_lcm_drv,
#endif

#if defined(HX8369B_WVGA_DSI_VDO)
	&hx8369b_wvga_dsi_vdo_drv,
#endif

#if defined(HX8369B_QHD_DSI_VDO)
	&hx8389b_qhd_dsi_vdo_drv,
#endif

#if defined(HX8389B_HD720_DSI_VDO)
	&hx8389b_hd720_dsi_vdo_drv,
#endif

#if defined(GN_SSD2825_SMD_S6E8AA)
	&gn_ssd2825_smd_s6e8aa,
#endif
#if defined(HX8369_TM_DSI)
	&hx8369_dsi_tm_lcm_drv,
#endif

#if defined(HX8369_BLD_DSI)
	&hx8369_dsi_bld_lcm_drv,
#endif

#if defined(HJ080IA)
	&hj080ia_lcm_drv,
#endif

#if defined(HJ101NA02A)
	&hj101na02a_lcm_drv,
#endif

#if defined(HJ101NA02A_8135)
	&hj101na02a_8135_lcm_drv,
#endif

#if defined(HSD070PFW3)
	&hsd070pfw3_lcm_drv,
#endif

#if defined(HSD070PFW3_8135)
	&hsd070pfw3_8135_lcm_drv,
#endif

#if defined(EJ101IA)
	&ej101ia_lcm_drv,
#endif

#if defined(SCF0700M48GGU02)
	&scf0700m48ggu02_lcm_drv,
#endif

#if defined(OTM1280A_HD720_DSI_CMD)
	&otm1280a_hd720_dsi_cmd_drv,
#endif

#if defined(OTM8018B_DSI_VDO)
	&otm8018b_dsi_vdo_lcm_drv,
#endif

#if defined(NT35512_DSI_VDO)
	&nt35512_dsi_vdo_lcm_drv,
#endif

#if defined(NT35512_WVGA_DSI_VDO_BOE)
	&nt35512_wvga_dsi_vdo_boe_drv,
#endif

#if defined(HX8389C_DSI_VDO)
	&hx8389c_dsi_vdo_lcm_drv,
#endif

#if defined(HX8392A_DSI_CMD)
	&hx8392a_dsi_cmd_lcm_drv,
#endif

#if defined(HX8392A_DSI_CMD_3LANE)
	&hx8392a_dsi_cmd_3lane_lcm_drv,
#endif

#if defined(HX8392A_DSI_CMD_WVGA)
	&hx8392a_dsi_cmd_wvga_lcm_drv,
#endif

#if defined(HX8392A_DSI_CMD_FWVGA)
	&hx8392a_dsi_cmd_fwvga_lcm_drv,
#endif

#if defined(HX8392A_DSI_CMD_QHD)
	&hx8392a_dsi_cmd_qhd_lcm_drv,
#endif

#if defined(HX8392A_DSI_VDO)
	&hx8392a_dsi_vdo_lcm_drv,
#endif

#if defined(HX8392A_DSI_VDO_2LANE)
	&hx8392a_dsi_vdo_2lane_lcm_drv,
#endif

#if defined(HX8392A_DSI_VDO_3LANE)
	&hx8392a_dsi_vdo_3lane_lcm_drv,
#endif

#if defined(NT35516_QHD_DSI_CMD_IPSBOE)
	&nt35516_qhd_dsi_cmd_ipsboe_lcm_drv,
#endif

#if defined(NT35516_QHD_DSI_CMD_IPSBOE_WVGA)
	&nt35516_qhd_dsi_cmd_ipsboe_wvga_lcm_drv,
#endif

#if defined(NT35516_QHD_DSI_CMD_IPSBOE_FWVGA)
	&nt35516_qhd_dsi_cmd_ipsboe_fwvga_lcm_drv,
#endif

#if defined(NT35516_QHD_DSI_CMD_IPS9K1431)
	&nt35516_qhd_dsi_cmd_ips9k1431_drv,
#endif

#if defined(NT35516_QHD_DSI_CMD_TFT9K1342)
	&nt35516_qhd_dsi_cmd_tft9k1342_drv,
#endif

#if defined(NT35516_QHD_DSI_VEDIO)
	&nt35516_qhd_rav4_lcm_drv,
#endif

#if defined(BP070WS1)
	&bp070ws1_lcm_drv,
#endif

#if defined(BP101WX1)
	&bp101wx1_lcm_drv,
#endif

#if defined(BP101WX1_N)
	&bp101wx1_n_lcm_drv,
#endif

#if defined(CM_N070ICE_DSI_VDO)
	&cm_n070ice_dsi_vdo_lcm_drv,
#endif

#if defined(CM_N070ICE_DSI_VDO_MT8135)
	&cm_n070ice_dsi_vdo_mt8135_lcm_drv,
#endif

#if defined(CM_OTC3108BH161_DSI_VDO)
	&cm_otc3108bhv161_dsi_vdo_lcm_drv,
#endif
#if defined(NT35510_FWVGA)
	&nt35510_fwvga_lcm_drv,
#endif

#if defined(R63311_FHD_DSI_VDO_SHARP)
	&r63311_fhd_dsi_vdo_sharp_lcm_drv,
#endif

#if defined(R81592_HVGA_DSI_CMD)
	&r81592_hvga_dsi_cmd_drv,
#endif

#if defined(RM68190_QHD_DSI_VDO)
	&rm68190_dsi_vdo_lcm_drv,
#endif

#if defined(NT35596_FHD_DSI_VDO_TRULY)
	&nt35596_fhd_dsi_vdo_truly_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_VDO_TRULY)
	&nt35595_fhd_dsi_vdo_truly_lcm_drv,
#endif

#if defined(R63319_WQHD_DSI_VDO_TRULY)
	&r63319_wqhd_dsi_vdo_truly_lcm_drv,
#endif


#if defined(NT35598_WQHD_DSI_VDO_TRULY)
	&nt35598_wqhd_dsi_vdo_truly_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_TPS65132)
	&nt35595_fhd_dsi_cmd_truly_tps65132_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_VDO_TRULY_TPS65132)
	&nt35595_fhd_dsi_vdo_truly_tps65132_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_TPS65132_720P)
	&nt35595_fhd_dsi_cmd_truly_tps65132_720p_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY)
	&nt35595_fhd_dsi_cmd_truly_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358)
	&nt35595_fhd_dsi_cmd_truly_nt50358_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_VDO_TRULY_NT50358)
	&nt35595_fhd_dsi_vdo_truly_nt50358_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_720P)
	&nt35595_fhd_dsi_cmd_truly_nt50358_720p_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_QHD)
	&nt35595_fhd_dsi_cmd_truly_nt50358_qhd_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_FWVGA)
	&nt35595_fhd_dsi_cmd_truly_nt50358_fwvga_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_WVGA)
	&nt35595_fhd_dsi_cmd_truly_nt50358_wvga_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_6735)
	&nt35595_fhd_dsi_cmd_truly_nt50358_6735_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_6735_720P)
	&nt35595_fhd_dsi_cmd_truly_nt50358_6735_720p_lcm_drv,
#endif

#if defined(NT35596_FHD_DSI_VDO_YASSY)
	&nt35596_fhd_dsi_vdo_yassy_lcm_drv,
#endif

#if defined(NT35596_HD720_DSI_VDO_TRULY_TPS65132)
	&nt35596_hd720_dsi_vdo_truly_tps65132_lcm_drv,
#endif

#if defined(AUO_B079XAT02_DSI_VDO)
	&auo_b079xat02_dsi_vdo_lcm_drv,
#endif

#if defined(OTM9608_WVGA_DSI_CMD)
	&otm9608_wvga_dsi_cmd_drv,
#endif

#if defined(OTM9608_FWVGA_DSI_CMD)
	&otm9608_fwvga_dsi_cmd_drv,
#endif

#if defined(OTM9608_QHD_DSI_CMD)
	&otm9608_qhd_dsi_cmd_drv,
#endif

#if defined(OTM9608_QHD_DSI_VDO)
	&otm9608_qhd_dsi_vdo_drv,
#endif

#if defined(OTM8009A_FWVGA_DSI_CMD_TIANMA)
	&otm8009a_fwvga_dsi_cmd_tianma_lcm_drv,
#endif

#if defined(OTM8009A_FWVGA_DSI_VDO_TIANMA)
	&otm8009a_fwvga_dsi_vdo_tianma_lcm_drv,
#endif

#if defined(HX8389B_QHD_DSI_VDO_TIANMA)
	&hx8389b_qhd_dsi_vdo_tianma_lcm_drv,
#endif
#if defined(HX8389B_QHD_DSI_VDO_TIANMA055XDHP)
	&hx8389b_qhd_dsi_vdo_tianma055xdhp_lcm_drv,
#endif

#if defined(CPT_CLAA101FP01_DSI_VDO)
	&cpt_claa101fp01_dsi_vdo_lcm_drv,
#endif

#if defined(CPT_CLAA101FP01_DSI_VDO_8163)
	&cpt_claa101fp01_dsi_vdo_8163_lcm_drv,
#endif

#if defined(IT6151_EDP_DSI_VIDEO_SHARP)
	&it6151_edp_dsi_video_sharp_lcm_drv,
#endif

#if defined(CPT_CLAP070WP03XG_SN65DSI83)
	&cpt_clap070wp03xg_sn65dsi83_lcm_drv,
#endif
#if defined(NT35520_HD720_DSI_CMD_TM)
	&nt35520_hd720_tm_lcm_drv,
#endif
#if defined(NT35520_HD720_DSI_CMD_BOE)
	&nt35520_hd720_boe_lcm_drv,
#endif
#if defined(NT35521_HD720_DSI_VDO_BOE)
	&nt35521_hd720_dsi_vdo_boe_lcm_drv,
#endif
#if defined(NT35521_HD720_DSI_VIDEO_TM)
	&nt35521_hd720_tm_lcm_drv,
#endif
#if defined(R69338_HD720_DSI_VDO_JDI_DW8755A)
	&r69338_hd720_dsi_vdo_jdi_dw8755a_drv,
#endif
#if defined(H070D_18DM)
	&h070d_18dm_lcm_drv,
#endif
#if defined(R69429_WUXGA_DSI_VDO)
	&r69429_wuxga_dsi_vdo_lcm_drv,
#endif

#if defined(HX8394D_HD720_DSI_VDO_TIANMA)
	&hx8394d_hd720_dsi_vdo_tianma_lcm_drv,
#endif

#if defined(HX8394A_HD720_DSI_VDO_TIANMA)
	&hx8394a_hd720_dsi_vdo_tianma_lcm_drv,
#endif

#if defined(R69429_WUXGA_DSI_CMD)
	&r69429_wuxga_dsi_cmd_lcm_drv,
#endif

#if defined(RM68210_HD720_DSI_UFOE_CMD)
	&rm68210_hd720_dsi_ufoe_cmd_lcm_drv,
#endif

#if defined(CPT_CLAP070WP03XG_LVDS)
	&cpt_clap070wp03xg_lvds_lcm_drv,
#endif

#if defined(OTM8018B_DSI_VDO_L72)
	&otm8018b_dsi_vdo_l72_lcm_drv,
#endif

#if defined(HX8369_DSI_CMD_6571)
	&hx8369_dsi_cmd_6571_lcm_drv,
#endif

#if defined(HX8369_DSI_VDO_6571)
	&hx8369_dsi_vdo_6571_lcm_drv,
#endif

#if defined(RX_498HX_615B_82)
	&RX_498HX_615B_82_lcm_drv,
#endif

#if defined(HX8369_DBI_6571)
	&hx8369_dbi_6571_lcm_drv,
#endif

#if defined(RX_498HX_615B)
	&RX_498HX_615B_lcm_drv,
#endif

#if defined(HX8369_DPI_6571)
	&hx8369_dpi_6571_lcm_drv,
#endif

#if defined(HX8389B_QHD_DSI_VDO_LGD)
	&hx8389b_qhd_dsi_vdo_lgd_lcm_drv,
#endif

#if defined(NT35510_DSI_CMD_6571)
	&nt35510_dsi_cmd_6571_lcm_drv,
#endif

#if defined(NT35510_DSI_CMD_6571_HVGA)
	&nt35510_dsi_cmd_6571_hvga_lcm_drv,
#endif

#if defined(NT35510_DSI_CMD_6571_QVGA)
	&nt35510_dsi_cmd_6571_qvga_lcm_drv,
#endif

#if defined(NT35510_DSI_VDO_6571)
	&nt35510_dsi_vdo_6571_lcm_drv,
#endif

#if defined(NT35510_DBI_6571)
	&nt35510_dbi_6571_lcm_drv,
#endif

#if defined(NT35510_DPI_6571)
	&nt35510_dpi_6571_lcm_drv,
#endif

#if defined(NT35590_DSI_CMD_6571_FWVGA)
	&nt35590_dsi_cmd_6571_fwvga_lcm_drv,
#endif

#if defined(NT35590_DSI_CMD_6571_QHD)
	&nt35590_dsi_cmd_6571_qhd_lcm_drv,
#endif

#if defined(NT35517_QHD_DSI_VIDEO)
	&nt35517_qhd_dsi_vdo_lcm_drv,
#endif

#if defined(IT6151_FHD_EDP_DSI_VIDEO_AUO)
	&it6151_fhd_edp_dsi_video_auo_lcm_drv,
#endif

#if defined(A080EAN01_DSI_VDO)
	&a080ean01_dsi_vdo_lcm_drv,
#endif

#if defined(IT6121_G156XW01V1_LVDS_VDO)
	&it6121_g156xw01v1_lvds_vdo_lcm_drv,
#endif

#if defined(ILI9806C_DSI_VDO_DJN_FWVGA)
	&ili9806c_dsi_vdo_djn_fwvga_lcm_drv,
#endif

#if defined(R69338_HD720_DSI_VDO_JDI)
	&r69338_hd720_dsi_vdo_jdi_drv,
#endif

#if defined(R69338_HD720_5IN_DSI_VDO_JDI_DW8768)
	&r69338_hd720_5in_dsi_vdo_jdi_dw8768_drv,
#endif

#if defined(DB7436_DSI_VDO_FWVGA)
	&db7436_dsi_vdo_fwvga_drv,
#endif

#if defined(R63417_FHD_DSI_CMD_TRULY_NT50358)
	&r63417_fhd_dsi_cmd_truly_nt50358_lcm_drv,
#endif

#if defined(R63417_FHD_DSI_CMD_TRULY_NT50358_720P)
	&r63417_fhd_dsi_cmd_truly_nt50358_720p_lcm_drv,
#endif

#if defined(R63417_FHD_DSI_CMD_TRULY_NT50358_QHD)
	&r63417_fhd_dsi_cmd_truly_nt50358_qhd_lcm_drv,
#endif

#if defined(R63417_FHD_DSI_VDO_TRULY_NT50358)
	&r63417_fhd_dsi_vdo_truly_nt50358_lcm_drv,
#endif

#if defined(R63419_WQHD_TRULY_PHANTOM_2K_CMD_OK)
	&r63419_wqhd_truly_phantom_cmd_lcm_drv,
#endif

#if defined(R63419_WQHD_TRULY_PHANTOM_2K_CMD_OK_MT6797)
	&r63419_wqhd_truly_phantom_cmd_lcm_drv,
#endif

#if defined(R63419_WQHD_TRULY_PHANTOM_2K_VDO_OK)
	&r63419_wqhd_truly_phantom_vdo_lcm_drv,
#endif

#if defined(R63419_WQHD_TRULY_PHANTOM_2K_VDO_OK_MT6797)
	&r63419_wqhd_truly_phantom_vdo_lcm_drv,
#endif

#if defined(R63419_FHD_TRULY_PHANTOM_2K_CMD_OK)
	&r63419_fhd_truly_phantom_lcm_drv,
#endif

#if defined(R63419_FHD_TRULY_PHANTOM_2K_CMD_OK_MT6797)
	&r63419_fhd_truly_phantom_lcm_drv,
#endif

#if defined(R63423_WQHD_TRULY_PHANTOM_2K_CMD_OK)
	&r63423_wqhd_truly_phantom_lcm_drv,
#endif

#if defined(NT35523_WXGA_DSI_VDO_BOE)
	&nt35523_wxga_dsi_vdo_boe_lcm_drv,
#endif

#if defined(NT35523_WSVGA_DSI_VDO_BOE)
	&nt35523_wsvga_dsi_vdo_boe_lcm_drv,
#endif

#if defined(EK79023_DSI_WSVGA_VDO)
	&ek79023_dsi_wsvga_vdo_lcm_drv,
#endif

#if defined(OTM9605A_QHD_DSI_VDO)
	&otm9605a_qhd_dsi_vdo_drv,
#endif

#if defined(OTM1906A_FHD_DSI_CMD_AUTO)
	&otm1906a_fhd_dsi_cmd_auto_lcm_drv,
#endif

#if defined(NT35532_FHD_DSI_VDO_SHARP)
	&nt35532_fhd_dsi_vdo_sharp_lcm_drv,
#endif

#if defined(CLAP070WP03XG_LVDS_8163)
	&clap070wp03xg_lvds_8163_lcm_drv,
#endif

#if defined(S6D7AA0_WXGA_DSI_VDO)
	&s6d7aa0_wxga_dsi_vdo_lcm_drv,
#endif

#if defined(SY20810800210132_WUXGA_DSI_VDO)
	&sy20810800210132_wuxga_dsi_vdo_lcm_drv,
#endif

#if defined(OTM1906B_FHD_DSI_CMD_JDI_TPS65132)
	&otm1906b_fhd_dsi_cmd_jdi_tps65132_lcm_drv,
#endif

#if defined(OTM1906B_FHD_DSI_CMD_JDI_TPS65132_MT6797)
	&otm1906b_fhd_dsi_cmd_jdi_tps65132_mt6797_lcm_drv,
#endif

#if defined(OTM1906B_FHD_DSI_VDO_JDI_TPS65132_MT6797)
	&otm1906b_fhd_dsi_vdo_jdi_tps65132_mt6797_lcm_drv,
#endif

#if defined(HX8394C_WXGA_DSI_VDO)
	&hx8394c_wxga_dsi_vdo_lcm_drv,
#endif

#if defined(IT6151_LP079QX1_EDP_DSI_VIDEO_8163EVB)
	&it6151_lp079qx1_edp_dsi_video_8163evb_lcm_drv,
#endif

#if defined(NT35510_DSI_CMD)
	&nt35510_dsi_cmd_lcm_drv,
#endif

#if defined(NT35695_FHD_DSI_CMD_TRULY_NT50358)
	&nt35695_fhd_dsi_cmd_truly_nt50358_lcm_drv,
#endif

#if defined(NT35695_FHD_DSI_VDO_TRULY_NT50358)
	&nt35695_fhd_dsi_vdo_truly_nt50358_lcm_drv,
#endif

#if defined(NT35695_FHD_DSI_CMD_TRULY_NT50358_720P)
	&nt35695_fhd_dsi_cmd_truly_nt50358_720p_lcm_drv,
#endif

#if defined(NT35695_FHD_DSI_CMD_TRULY_NT50358_QHD)
	&nt35695_fhd_dsi_cmd_truly_nt50358_qhd_lcm_drv,
#endif

#if defined(NT35695_FHD_DSI_CMD_TRULY_NT50358_LANESWAP)
	&nt35695_fhd_dsi_cmd_truly_nt50358_laneswap_lcm_drv,
#endif

#if defined(NT35695_FHD_DSI_VDO_TRULY_NT50358_LANESWAP)
	&nt35695_fhd_dsi_vdo_truly_nt50358_laneswap_lcm_drv,
#endif

#if defined(RM69032_DSI_CMD)
	&rm69032_dsi_cmd_lcm_drv,
#endif

#if defined(ST7789H2_DBI)
	&st7789h2_dbi_lcm_drv,
#endif

#if defined(CM_N070ICE_DSI_VDO_MT8173)
	&cm_n070ice_dsi_vdo_mt8173_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_EXTERN)
	&nt35595_fhd_dsi_cmd_truly_nt50358_extern_lcm_drv,
#endif

#if defined(R69429_WQXGA_DSI_VDO)
	&r69429_wqxga_dsi_vdo_lcm_drv,
#endif

#if defined(HX8394C_WXGA_DSI_VDO)
	&hx8394c_wxga_dsi_vdo_lcm_drv,
#endif

#if defined(NT35595_TRULY_FHD_DSI_VDO)
	&nt35595_truly_fhd_dsi_vdo_lcm_drv,
#endif

#if defined(B080UAN01_2_WUXGA_DSI_VDO)
	&b080uan01_2_wuxga_dsi_vdo_lcm_drv,
#endif

#if defined(NT36850_WQHD_DSI_2K_CMD)
	&nt36850_wqhd_dsi_2k_cmd_lcm_drv,
#endif

#if defined(S6E3HA3_WQHD_2K_CMD)
	&s6e3ha3_wqhd_2k_cmd_lcm_drv,
#endif

#if defined(NT35595_FHD_DSI_CMD_TRULY_NT50358_720P_EXTERN)
	&nt35595_fhd_dsi_cmd_truly_nt50358_720p_extern_lcm_drv,
#endif

#if defined(NT35695_FHD_DSI_VDO_TRULY_NT50358_720P)
	&nt35695_fhd_dsi_vdo_truly_nt50358_720p_lcm_drv,
#endif
//#BIRD BEGIN
#if defined(ST7701_FWVGA_HSDTN_VDO_YIXINDAG5_FOR_E5011)
	&st7701_fwvga_hsdtn_vdo_yixindag5_for_e5011_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_IVO_VDO_BAOXU_FOR_E5011)
	&ili9806e_fwvga_ivo_vdo_baoxu_for_e5011_lcm_drv,
#endif

#if defined(NT35521_HD720_DSI_VDO_KYD_BOE_FOR_MX2109E_SOP_P2)
	&nt35521_hd720_dsi_vdo_kyd_boe_for_mx2109e_sop_p2_lcm_drv,
#endif

#if defined(RM68200_HD720_DSI_VDO_AUO_WCL_W502118AAA_MX2109E_SOP_P2)
	&rm68200_hd720_dsi_vdo_auo_wcl_w502118aaa_mx2109e_sop_p2_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX50006139C_MX2135F_SZBD_E5018)
           &ili9881c_hd720_dsi_vdo_ivo_bx_bx50006139c_mx2135f_szbd_e5018_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX60000639C_MX2132E_BER_S6002)
           &ili9881c_hd720_dsi_vdo_ivo_bx_bx60000639c_mx2132e_ber_s6002_lcm_drv,
#endif

#if defined(ILI9807_QHD_DSI_VDO_IVO_BX_MX2135F_SZBD_E5505)
           &ili9807_qhd_dsi_vdo_ivo_bx_mx2135f_szbd_e5505_lcm_drv,
#endif

#if defined(FL10802_FWVGA_DSI_VDO_BOE_JJ_FPCT50V254V1_MX2109F_ARES_A5085)
           &fl10802_fwvga_dsi_vdo_boe_jj_fpct50v254v1_mx2109f_ares_a5085_lcm_drv,
#endif

#if defined(IlI9806E_FWVGA_DSI_VDO_IVO_BX_BX49502225C_MX2109F_A5023)
           &ili9806e_fwvga_dsi_vdo_ivo_bx_bx49502225c_mx2109f_a5023_lcm_drv,
#endif

#if defined(FL10802_FWVGA_DSI_VDO_BOE_JJ_FPCT50V254V1_MX2109F_ARES_A5085_FOR_MX2132ER)
	&fl10802_fwvga_dsi_vdo_boe_jj_fpct50v254v1_mx2109f_ares_a5085_for_mx2132er_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_DSI_VDO_IVO_BX_BX49502225C_MX2109F_A5023_FOR_MX2132ER)
	&ili9806e_fwvga_dsi_vdo_ivo_bx_bx49502225c_mx2109f_a5023_for_mx2132er_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_YIXINDA_MX2135F_E5505)
           &ili9881c_hd720_dsi_vdo_ivo_yixinda_mx2135f_e5505_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX50006139C_MX2135FA_SZBD_E5018_SA1)
           &ili9881c_hd720_dsi_vdo_ivo_bx_bx50006139c_mx2135fa_szbd_e5018_sa1_lcm_drv,
#endif

#if defined(ILI9881P_HD720_DSI_VDO_PANDA_BX_BX500125_MX2135F_SZBD_E5018)
           &ili9881p_hd720_dsi_vdo_panda_bx_bx500125_mx2135f_szbd_e5018_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_BOE_HLT_HT55061_MX2103)
	&ili9881c_hd720_dsi_vdo_boe_hlt_ht55061_mx2103_lcm_drv,
#endif

#if defined(OTM8012A_FWVGA_DSI_CMD_BOE_YXD_Y83330_72M_MX2103)
	&otm8012a_fwvga_dsi_cmd_boe_yxd_y83330_72m_mx2103_lcm_drv,
#endif

#if defined(RM68172_FWVGA_DSI_VDO_TM_BAOXU_TM045YDH2600_MX2103_SMT)
	&rm68172_fwvga_dsi_vdo_tm_baoxu_tm045ydh2600_mx2103_smt_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_DSI_VDO_BOE_BX_BX45003725C_MX2103_SMT)
	&ili9806e_fwvga_dsi_vdo_boe_bx_bx45003725c_mx2103_smt_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HZ_FHD55089A_IPS_MX2103_HBBD_M6)
	&ili9881c_hd720_dsi_vdo_hsd_hz_fhd55089a_ips_mx2103_hbbd_m6_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HLT_HTC055H036_MX2103)
	&ili9881c_hd720_dsi_vdo_hsd_hlt_htc055h036_mx2103_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_YIXINDA_MX2135F_SZBD_E5017)
	&ili9881c_hd720_dsi_vdo_ivo_yixinda_mx2135f_szbd_e5017_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_WSS_MVG6001_MX2132E_BER_S6002)
           &ili9881c_hd720_dsi_vdo_hsd_wss_mvg6001_mx2132e_ber_s6002_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX55003139C_MX2120E_BER_S5566)
	&ili9881c_hd720_dsi_vdo_ivo_bx_bx55003139c_mx2120e_ber_s5566_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_AUO_KYD__MX2120E_BER_S5566)
	&ili9881c_hd720_dsi_vdo_auo_kyd__mx2120e_ber_s5566_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_BOE_VDO_YIXINDA_FOR_E5011)
	&ili9806e_fwvga_boe_vdo_yixinda_for_e5011_lcm_drv,
#endif

#if defined(ST7701_FWVGA_HANCAI_VDO_YIXINDA_FOR_E5011)
	&st7701_fwvga_hancai_vdo_yixinda_for_e5011_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_LONGYU_MX2135F_E5505)
           &ili9881c_hd720_dsi_vdo_hsd_longyu_mx2135f_e5505_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_DSI_VDO_BOE_BX_BX45003725C_MX2116_D4513)
           &ili9806e_fwvga_dsi_vdo_boe_bx_bx45003725c_mx2116_d4513_lcm_drv,
#endif

#if defined(RM68172_FWVGA_DSI_VDO_TM_BAOXU_TM045YDH2600_MX2116_D4513)
           &rm68172_fwvga_dsi_vdo_tm_baoxu_tm045ydh2600_mx2116_d4513_lcm_drv,
#endif

#if defined(FL11281_HD720_DSI_VDO_CMO_GONGTAI_F050A13603_MX2126E_YAAO_X15)
	&fl11281_hd720_dsi_vdo_cmo_gongtai_f050a13603_mx2126e_yaao_x15_lcm_drv,
#endif

#if defined(JD9365_HD720_DSI_VDO_AUO_SR_C050SWA6_MX2126E_YAAO_X15)
	&jd9365_hd720_dsi_vdo_auo_sr_c050swa6_mx2126e_yaao_x15_lcm_drv,
#endif

#if defined(JD9365_HD720_DSI_VDO_CMI_SR_F050A13_MX2126E_YAAO_X15)
	&jd9365_hd720_dsi_vdo_cmi_sr_f050a13_mx2126e_yaao_x15_lcm_drv,
#endif

#if defined(RM68200_HD720_AUO_IPS_VDO_YIKUAILAI_FOR_E5011)
	&rm68200_hd720_auo_ips_vdo_yikuailai_for_e5011_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CTC_VDO_HLT_FOR_S45261)
	&st7701_fwvga_ctc_vdo_hlt_for_s45261_lcm_drv,
#endif

#if defined(JD9161_FWVGA_CTC_VDO_COE_FOR_S45261)
	&jd9161_fwvga_ctc_vdo_coe_for_s45261_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_QICAI_MX1091F_F511)
	&ili9881c_hd720_dsi_vdo_qicai_mx1091f_f511_lcm_drv,
#endif

#if defined(RM68200_HD720_DSI_VDO_GAOZHAN_MX1091F_F511)
	&rm68200_hd720_dsi_vdo_gaozhan_mx1091f_f511_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HANCAI_MEIJING_MX2135F_SZBD_E5017)
	&ili9881c_hd720_dsi_vdo_hancai_meijing_mx2135f_szbd_e5017_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX60000639C_MX2132E_BER_S6002DM)
           &ili9881c_hd720_dsi_vdo_ivo_bx_bx60000639c_mx2132e_ber_s6002dm_lcm_drv,
#endif

#if defined(ILI9806E_WVGA_HSB6C_VDO_HLT_HTB040W081A0_MX2131F_SZBD_I589)
	&ili9806e_wvga_hsb6c_vdo_hlt_htb040w081a0_mx2131f_szbd_i589_lcm_drv,
#endif


#if defined(ST7701_WVGA_CTC_VDO_YIXINDA_FOR_MX2131F_I589)
	&st7701_wvga_ctc_vdo_yixinda_for_mx2131f_i589_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_WSS_MVG6001_MX2132E_BER_S6002DM)
           &ili9881c_hd720_dsi_vdo_hsd_wss_mvg6001_mx2132e_ber_s6002dm_lcm_drv,
#endif

#if defined(JD9161BA_FWVGA_DSI_VDO_IVO_ZGD_T050K409FPC_MX2128E_ALS_A5066)
	   &jd9161ba_fwvga_dsi_vdo_ivo_zgd_t050k409fpc_mx2128e_als_a5066_lcm_drv,
#endif

#if defined(JD9161BA_FWVGA_DSI_VDO_IVO_IPS_C050SWYG_MX2128E_ARES_A5066)
	   &jd9161ba_fwvga_dsi_vdo_ivo_ips_c050swyg_mx2128e_ares_a5066_lcm_drv,
#endif

#if defined(OTM8019_FWVGA_HSD_VDO_COE_FOR_S45261)
	&otm8019_fwvga_hsd_vdo_coe_for_s45261_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HZ_FHD55089A_IPS_MX2103FD_HBBD_M6)
	&ili9881c_hd720_dsi_vdo_hsd_hz_fhd55089a_ips_mx2103fd_hbbd_m6_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HLT_HTC055H036_MX2103FD)
	&ili9881c_hd720_dsi_vdo_hsd_hlt_htc055h036_mx2103fd_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CTC_VDO_YIXINDA_FOR_E4506)
	&st7701_fwvga_ctc_vdo_yixinda_for_e4506_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_HSD_VDO_YIXINDA_FOR_E4506)
	&ili9806e_fwvga_hsd_vdo_yixinda_for_e4506_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_HSD_VDO_MEIJING_FOR_E5017)
	&ili9806e_fwvga_hsd_vdo_meijing_for_e5017_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CTC_VDO_HLT_FOR_S45269)
	&st7701_fwvga_ctc_vdo_hlt_for_s45269_lcm_drv,
#endif

#if defined(JD9161_FWVGA_CTC_VDO_COE_FOR_S45269)
	&jd9161_fwvga_ctc_vdo_coe_for_s45269_lcm_drv,
#endif

#if defined(OTM8019_FWVGA_HSD_VDO_COE_FOR_S45269)
	&otm8019_fwvga_hsd_vdo_coe_for_s45269_lcm_drv,
#endif

#if defined(ILI9806E_WVGA_HSB6C_VDO_HLT_MX2131F_SZBD_S45269_SMT)
	&ili9806e_wvga_hsb6c_vdo_hlt_mx2131f_szbd_s45269_smt_lcm_drv,
#endif

#if defined(ST7701_WVGA_CTC_VDO_YIXINDA_FOR_MX2131FP_S45269_SMT)
	&st7701_wvga_ctc_vdo_yixinda_for_mx2131fp_s45269_smt_lcm_drv,
#endif

#if defined(ILI9885_FHD_DSI_VDO_AUO_HZ_HFH55050FPCA_MX2103FD_HBBD_M6B)
	&ili9885_fhd_dsi_vdo_auo_hz_hfh55050fpca_mx2103fd_hbbd_m6b_lcm_drv,
#endif

#if defined(FL11281_HD720_DSI_VDO_CPT_GONGTAI_GT501710A_MX2126E_YAAO_X15)
	&fl11281_hd720_dsi_vdo_cpt_gongtai_gt501710a_mx2126e_yaao_x15_lcm_drv,
#endif

#if defined(JD9365_HD720_DSI_VDO_IVO_SR_ST050RIVD490_MX2126E_YAAO_X15)
	&jd9365_hd720_dsi_vdo_ivo_sr_st050rivd490_mx2126e_yaao_x15_lcm_drv,
#endif

#if defined(RM68200_HD720_DSI_VDO_CMI_CHUANGYU_XX_MX2128E_JD5002)
           &rm68200_hd720_dsi_vdo_cmi_chuangyu_xx_mx2128e_jd5002_lcm_drv,
#endif

#if defined(RM68200_HD720_DSI_VDO_CMI_CHUANGYU_CY50HDA203V1_MX2118B_JD_HT5001K)
           &rm68200_hd720_dsi_vdo_cmi_chuangyu_cy50hda203v1_mx2118b_jd_ht5001k_lcm_drv,
#endif


#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_C050S050K_MX2128E_JD_JD5002)
	&ili9881c_hd720_dsi_vdo_ivo_bx_c050s050k_mx2128e_jd_jd5002_lcm_drv,
#endif

#if defined(ILI9885_FHD_DSI_VDO_AUO_YIKUAILAI_FOR_W5503)
	&ili9885_fhd_dsi_vdo_auo_yikuailai_for_w5503_lcm_drv,
#endif

#if defined(NT35532_FHD_DSI_VDO_PANDA_YIXINDA_FOR_W5503)
	&nt35532_fhd_dsi_vdo_panda_yixinda_for_w5503_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_DSI_VDO_IVO_BX_BX49505325C_MX2150E_WPF_D5033)
           &ili9806e_fwvga_dsi_vdo_ivo_bx_bx49505325c_mx2150e_wpf_d5033_lcm_drv,
#endif

#if defined(ST7701_WVGA_BOE_VDO_YIXINDA_FOR_E4008)
	&st7701_wvga_boe_vdo_yixinda_for_e4008_lcm_drv,
#endif

#if defined(ILI9806E_WVGA_TN_VDO_YIXINDA_FOR_E4008)
	&ili9806e_wvga_tn_vdo_yixinda_for_e4008_lcm_drv,
#endif

#if defined(RM68200_HD720_DSI_VDO_AUO_WCL_F055A03601_MX2126_SOP_C6)
	&rm68200_hd720_dsi_vdo_auo_wcl_f055a03601_mx2126_sop_c6_lcm_drv,
#endif

#if defined(NT35521SH_HD720_DSI_VDO_KYD_BOE_BV050HDEN40_MX2126_SOP_C6)
	&nt35521sh_hd720_dsi_vdo_kyd_boe_bv050hden40_mx2126_sop_c6_lcm_drv,
#endif

#if defined(ILI9807_QHD_DSI_VDO_AUO_WCL_W552200AAA_MX2126E_SOP_C6)
	&ili9807_qhd_dsi_vdo_auo_wcl_w552200aaa_mx2126e_sop_c6_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HZ_HHD50344_MX2128E_YXD_E5001)
	&ili9881c_hd720_dsi_vdo_hsd_hz_hhd50344_mx2128e_yxd_e5001_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX50005525_MX1091E_WPF_D5028B)
           &ili9881c_hd720_dsi_vdo_ivo_bx_bx50005525_mx1091e_wpf_d5028b_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HZ_HHD50344_MX2128E_V02_YXD_E5001)
           &ili9881c_hd720_dsi_vdo_hsd_hz_hhd50344_mx2128e_v02_yxd_e5001_lcm_drv,
#endif

#if defined(RM68200_HD720_DSI_VDO_CMI_CHUANGYU_XX_MX2128E_V02_JD5002)
           &rm68200_hd720_dsi_vdo_cmi_chuangyu_xx_mx2128e_v02_jd5002_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_C050S050K_MX2128E_V02_JD_JD5002)
           &ili9881c_hd720_dsi_vdo_ivo_bx_c050s050k_mx2128e_v02_jd_jd5002_lcm_drv,
#endif

#if defined(NT35521_HD720_DSI_VDO_CMI_KYD_FPC055NNH161A1_MX2151E_SFT_J5501)
	&nt35521_hd720_dsi_vdo_cmi_kyd_fpc055nnh161a1_mx2151e_sft_j5501_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CTC_VDO_TYF_FOR_CQ4011)
           &st7701_fwvga_ctc_vdo_tyf_for_cq4011_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CTC_VDO_COE_FOR_CQ4011)
           &st7701_fwvga_ctc_vdo_coe_for_cq4011_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_IVO_VDO_YIXINDA_FOR_E5017)
           &ili9806e_fwvga_ivo_vdo_yixinda_for_e5017_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_HX_C0340A_MX2521E_SOP_N1)
	&ili9881c_hd720_dsi_vdo_ivo_hx_c0340a_mx2521e_sop_n1_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_HZ_HHD50421_MX2521E_SOP_N1)
	&ili9881c_hd720_dsi_vdo_ivo_hz_hhd50421_mx2521e_sop_n1_lcm_drv,
#endif

#if defined(JD9161BA_FWVGA_DSI_VDO_IVO_IPS_C050SWYG_MX2150E_WPF_D5033)
           &jd9161ba_fwvga_dsi_vdo_ivo_ips_c050swyg_mx2150e_wpf_d5033_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HZ_HHD55144_MX2132E_ARES_A5508)
	&ili9881c_hd720_dsi_vdo_hsd_hz_hhd55144_mx2132e_ares_a5508_lcm_drv,
#endif

#if defined(JD9365_HD720_DSI_VDO_IVO_BX_55008030B_MX2521E_V01_WPF_D5517)
           &jd9365_hd720_dsi_vdo_ivo_bx_55008030b_mx2521e_v01_wpf_d5517_lcm_drv,
#endif

#if defined(JD9365_HD720_DSI_VDO_IVO_BX_50071_MX2521E_V01_WPF_D5036)
           &jd9365_hd720_dsi_vdo_ivo_bx_50071_mx2521e_v01_wpf_d5036_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CTC_VDO_YXD_FOR_S45261)
           &st7701_fwvga_ctc_vdo_yxd_for_s45261_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CTC_VDO_YXD_FOR_S45269)
           &st7701_fwvga_ctc_vdo_yxd_for_s45269_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_BOE_HLT_HT55061_MX2109F_L7P)
	&ili9881c_hd720_dsi_vdo_boe_hlt_ht55061_mx2109f_l7p_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HZ_FHD55089A_IPS_MX2109F_L7P)
	&ili9881c_hd720_dsi_vdo_hsd_hz_fhd55089a_ips_mx2109f_l7p_lcm_drv,
#endif

#if defined(ILI9885_QHD_DSI_VDO_XP_DT_DT552XXAV0_MX2120E_HDS_S702)
	&ili9885_qhd_dsi_vdo_xp_dt_dt552xxav0_mx2120e_hds_s702_lcm_drv,
#endif

#if defined(ILI9885_QHD_DSI_VDO_XP_DT_DT552XXUV0_MX2120E_HDS_S702)
	&ili9885_qhd_dsi_vdo_xp_dt_dt552xxuv0_mx2120e_hds_s702_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX55004339C_MX2120E_WPF_D5513)
	&ili9881c_hd720_dsi_vdo_ivo_bx_bx55004339c_mx2120e_wpf_d5513_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_HSD_VDO_YIXINDA_FOR_E5017)
           &ili9806e_fwvga_hsd_vdo_yixinda_for_e5017_lcm_drv,
#endif
#if defined(ILI9806E_WVGA_CMI_VDO_YIXINDA_FOR_E4008)
	&ili9806e_wvga_cmi_vdo_yixinda_for_e4008_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_CMI_LONGYU_MX2151E_SFT_J5501)
           &ili9881c_hd720_dsi_vdo_cmi_longyu_mx2151e_sft_j5501_lcm_drv,
#endif

#if defined(FL11281_HD720_DSI_VDO_CPT_GONGTAI_XXX_MX2132ER_SY_F10)
           &fl11281_hd720_dsi_vdo_cpt_gongtai_xxx_mx2132er_sy_f10_lcm_drv,
#endif

#if defined(SSD2075_HD720_DSI_VDO_LG_QT500HD045SH01_MX2132ER_SY_F10)
           &ssd2075_hd720_dsi_vdo_lg_qt500hd045sh01_mx2132er_sy_f10_lcm_drv,
#endif

#if defined(SSD2075_HD720_DSI_VDO_LG_QT470HD028SH02_MX2521E_YAAO_X28)
	&ssd2075_hd720_dsi_vdo_lg_qt470hd028sh02_mx2521e_yaao_x28_lcm_drv,
#endif

#if defined(JD9365_HD720_DSI_VDO_HSD_ZMA_HSD047FHW1_MX2521E_YAAO_X28)
	&jd9365_hd720_dsi_vdo_hsd_zma_hsd047fhw1_mx2521e_yaao_x28_lcm_drv,
#endif


#if defined(ILI9806E_FWVGA_DSI_VDO_IVO_BX_BX49505325C_MX2150E_JY_A5010)
           &ili9806e_fwvga_dsi_vdo_ivo_bx_bx49505325c_mx2150e_jy_a5010_lcm_drv,
#endif

#if defined(ST7701_FWVGA_CMI_VDO_LONGYU_FOR_E5017)
	&st7701_fwvga_cmi_vdo_longyu_for_e5017_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_IVO_BX_BX60000639C_MX2132F_BER_S6002)
           &ili9881c_hd720_dsi_vdo_ivo_bx_bx60000639c_mx2132f_ber_s6002_lcm_drv,
#endif

#if defined(ILI9881C_HD720_DSI_VDO_HSD_HHD50467_MX2150P_HBBD_H1)
	&ili9881c_hd720_dsi_vdo_hsd_hhd50467_mx2150p_hbbd_h1_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_DSI_VDO_IVO_BX_BX49505325C_MX2150P_WPF_D5033)
           &ili9806e_fwvga_dsi_vdo_ivo_bx_bx49505325c_mx2150p_wpf_d5033_lcm_drv,
#endif

#if defined(JD9161BA_FWVGA_DSI_VDO_IVO_IPS_C050SWYG_MX2150P_WPF_D5033)
           &jd9161ba_fwvga_dsi_vdo_ivo_ips_c050swyg_mx2150p_wpf_d5033_lcm_drv,
#endif

#if defined(ILI9806E_FWVGA_DSI_VDO_IVO_BX_BX49505325C_MX2150P_JY_A5010)
           &ili9806e_fwvga_dsi_vdo_ivo_bx_bx49505325c_mx2150p_jy_a5010_lcm_drv,
#endif

#if defined(JD9365_HD720_DSI_VDO_IVO_SONGRUI_050RIVD94_MX2521E_YAAO_X27)
           &jd9365_hd720_dsi_vdo_ivo_songrui_050rivd94_mx2521e_yaao_x27_lcm_drv,
#endif

#if defined(RM68200_HD720_DSI_VDO_INL_ZMA_F050A13_MX2521E_YAAO_X27)
	   &rm68200_hd720_dsi_vdo_inl_zma_f050a13_mx2521e_yaao_x27_lcm_drv,
#endif
#if defined(ST7703_HD720_DSI_VDO_LG_QT470HD074SH01_MX2521E_YAAO_X28)
	&st7703_hd720_dsi_vdo_lg_qt470hd074sh01_mx2521e_yaao_x28_lcm_drv,
#endif
//#BIRD END
};

unsigned char lcm_name_list[][128] = {
#if defined(HX8392A_DSI_CMD)
	"hx8392a_dsi_cmd",
#endif

#if defined(S6E3HA3_WQHD_2K_CMD)
	"s6e3ha3_wqhd_2k_cmd",
#endif

#if defined(HX8392A_DSI_VDO)
	"hx8392a_vdo_cmd",
#endif

#if defined(HX8392A_DSI_CMD_FWVGA)
	"hx8392a_dsi_cmd_fwvga",
#endif

#if defined(OTM9608_QHD_DSI_CMD)
	"otm9608a_qhd_dsi_cmd",
#endif

#if defined(OTM9608_QHD_DSI_VDO)
	"otm9608a_qhd_dsi_vdo",
#endif

#if defined(R63417_FHD_DSI_CMD_TRULY_NT50358)
	"r63417_fhd_dsi_cmd_truly_nt50358_drv",
#endif

#if defined(R63417_FHD_DSI_CMD_TRULY_NT50358_QHD)
	"r63417_fhd_dsi_cmd_truly_nt50358_qhd_drv",
#endif

#if defined(R63417_FHD_DSI_VDO_TRULY_NT50358)
	"r63417_fhd_dsi_vdo_truly_nt50358_drv",
#endif

#if defined(R63419_WQHD_TRULY_PHANTOM_2K_CMD_OK)
	"r63419_wqhd_truly_phantom_2k_cmd_ok",
#endif
};

#define LCM_COMPILE_ASSERT(condition) LCM_COMPILE_ASSERT_X(condition, __LINE__)
#define LCM_COMPILE_ASSERT_X(condition, line) LCM_COMPILE_ASSERT_XX(condition, line)
#define LCM_COMPILE_ASSERT_XX(condition, line) char assertion_failed_at_line_##line[(condition) ? 1 : -1]

unsigned int lcm_count = sizeof(lcm_driver_list) / sizeof(LCM_DRIVER *);
LCM_COMPILE_ASSERT(0 != sizeof(lcm_driver_list) / sizeof(LCM_DRIVER *));
#if defined(NT35520_HD720_DSI_CMD_TM) | defined(NT35520_HD720_DSI_CMD_BOE) | \
	defined(NT35521_HD720_DSI_VDO_BOE) | defined(NT35521_HD720_DSI_VIDEO_TM)
static unsigned char lcd_id_pins_value = 0xFF;

/**
 * Function:       which_lcd_module_triple
 * Description:    read LCD ID PIN status,could identify three status:highlowfloat
 * Input:           none
 * Output:         none
 * Return:         LCD ID1|ID0 value
 * Others:
 */
unsigned char which_lcd_module_triple(void)
{
	unsigned char  high_read0 = 0;
	unsigned char  low_read0 = 0;
	unsigned char  high_read1 = 0;
	unsigned char  low_read1 = 0;
	unsigned char  lcd_id0 = 0;
	unsigned char  lcd_id1 = 0;
	unsigned char  lcd_id = 0;
	/*Solve Coverity scan warning : check return value*/
	unsigned int ret = 0;

	/*only recognise once*/
	if (0xFF != lcd_id_pins_value)
		return lcd_id_pins_value;

	/*Solve Coverity scan warning : check return value*/
	ret = mt_set_gpio_mode(GPIO_DISP_ID0_PIN, GPIO_MODE_00);
	if (0 != ret)
		LCD_DEBUG("ID0 mt_set_gpio_mode fail\n");

	ret = mt_set_gpio_dir(GPIO_DISP_ID0_PIN, GPIO_DIR_IN);
	if (0 != ret)
		LCD_DEBUG("ID0 mt_set_gpio_dir fail\n");

	ret = mt_set_gpio_pull_enable(GPIO_DISP_ID0_PIN, GPIO_PULL_ENABLE);
	if (0 != ret)
		LCD_DEBUG("ID0 mt_set_gpio_pull_enable fail\n");

	ret = mt_set_gpio_mode(GPIO_DISP_ID1_PIN, GPIO_MODE_00);
	if (0 != ret)
		LCD_DEBUG("ID1 mt_set_gpio_mode fail\n");

	ret = mt_set_gpio_dir(GPIO_DISP_ID1_PIN, GPIO_DIR_IN);
	if (0 != ret)
		LCD_DEBUG("ID1 mt_set_gpio_dir fail\n");

	ret = mt_set_gpio_pull_enable(GPIO_DISP_ID1_PIN, GPIO_PULL_ENABLE);
	if (0 != ret)
		LCD_DEBUG("ID1 mt_set_gpio_pull_enable fail\n");

	/*pull down ID0 ID1 PIN*/
	ret = mt_set_gpio_pull_select(GPIO_DISP_ID0_PIN, GPIO_PULL_DOWN);
	if (0 != ret)
		LCD_DEBUG("ID0 mt_set_gpio_pull_select->Down fail\n");

	ret = mt_set_gpio_pull_select(GPIO_DISP_ID1_PIN, GPIO_PULL_DOWN);
	if (0 != ret)
		LCD_DEBUG("ID1 mt_set_gpio_pull_select->Down fail\n");

	/* delay 100ms , for discharging capacitance*/
	mdelay(100);
	/* get ID0 ID1 status*/
	low_read0 = mt_get_gpio_in(GPIO_DISP_ID0_PIN);
	low_read1 = mt_get_gpio_in(GPIO_DISP_ID1_PIN);
	/* pull up ID0 ID1 PIN */
	ret = mt_set_gpio_pull_select(GPIO_DISP_ID0_PIN, GPIO_PULL_UP);
	if (0 != ret)
		LCD_DEBUG("ID0 mt_set_gpio_pull_select->UP fail\n");

	ret = mt_set_gpio_pull_select(GPIO_DISP_ID1_PIN, GPIO_PULL_UP);
	if (0 != ret)
		LCD_DEBUG("ID1 mt_set_gpio_pull_select->UP fail\n");

	/* delay 100ms , for charging capacitance */
	mdelay(100);
	/* get ID0 ID1 status */
	high_read0 = mt_get_gpio_in(GPIO_DISP_ID0_PIN);
	high_read1 = mt_get_gpio_in(GPIO_DISP_ID1_PIN);

	if (low_read0 != high_read0) {
		/*float status , pull down ID0 ,to prevent electric leakage*/
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID0_PIN, GPIO_PULL_DOWN);
		if (0 != ret)
			LCD_DEBUG("ID0 mt_set_gpio_pull_select->Down fail\n");

		lcd_id0 = LCD_HW_ID_STATUS_FLOAT;
	} else if ((LCD_HW_ID_STATUS_LOW == low_read0) && (LCD_HW_ID_STATUS_LOW == high_read0)) {
		/*low status , pull down ID0 ,to prevent electric leakage*/
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID0_PIN, GPIO_PULL_DOWN);
		if (0 != ret)
			LCD_DEBUG("ID0 mt_set_gpio_pull_select->Down fail\n");

		lcd_id0 = LCD_HW_ID_STATUS_LOW;
	} else if ((LCD_HW_ID_STATUS_HIGH == low_read0) && (LCD_HW_ID_STATUS_HIGH == high_read0)) {
		/*high status , pull up ID0 ,to prevent electric leakage*/
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID0_PIN, GPIO_PULL_UP);
		if (0 != ret)
			LCD_DEBUG("ID0 mt_set_gpio_pull_select->UP fail\n");

		lcd_id0 = LCD_HW_ID_STATUS_HIGH;
	} else {
		LCD_DEBUG(" Read LCD_id0 error\n");
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID0_PIN, GPIO_PULL_DISABLE);
		if (0 != ret)
			LCD_DEBUG("ID0 mt_set_gpio_pull_select->Disbale fail\n");

		lcd_id0 = LCD_HW_ID_STATUS_ERROR;
	}


	if (low_read1 != high_read1) {
		/*float status , pull down ID1 ,to prevent electric leakage*/
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID1_PIN, GPIO_PULL_DOWN);
		if (0 != ret)
			LCD_DEBUG("ID1 mt_set_gpio_pull_select->Down fail\n");

		lcd_id1 = LCD_HW_ID_STATUS_FLOAT;
	} else if ((LCD_HW_ID_STATUS_LOW == low_read1) && (LCD_HW_ID_STATUS_LOW == high_read1)) {
		/*low status , pull down ID1 ,to prevent electric leakage*/
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID1_PIN, GPIO_PULL_DOWN);
		if (0 != ret)
			LCD_DEBUG("ID1 mt_set_gpio_pull_select->Down fail\n");

		lcd_id1 = LCD_HW_ID_STATUS_LOW;
	} else if ((LCD_HW_ID_STATUS_HIGH == low_read1) && (LCD_HW_ID_STATUS_HIGH == high_read1)) {
		/*high status , pull up ID1 ,to prevent electric leakage*/
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID1_PIN, GPIO_PULL_UP);
		if (0 != ret)
			LCD_DEBUG("ID1 mt_set_gpio_pull_select->UP fail\n");

		lcd_id1 = LCD_HW_ID_STATUS_HIGH;
	} else {

		LCD_DEBUG(" Read LCD_id1 error\n");
		ret = mt_set_gpio_pull_select(GPIO_DISP_ID1_PIN, GPIO_PULL_DISABLE);
		if (0 != ret)
			LCD_DEBUG("ID1 mt_set_gpio_pull_select->Disable fail\n");

		lcd_id1 = LCD_HW_ID_STATUS_ERROR;
	}
#ifdef BUILD_LK
	dprintf(CRITICAL, "which_lcd_module_triple,lcd_id0:%d\n", lcd_id0);
	dprintf(CRITICAL, "which_lcd_module_triple,lcd_id1:%d\n", lcd_id1);
#else
	LCD_DEBUG("which_lcd_module_triple,lcd_id0:%d\n", lcd_id0);
	LCD_DEBUG("which_lcd_module_triple,lcd_id1:%d\n", lcd_id1);
#endif
	lcd_id =  lcd_id0 | (lcd_id1 << 2);

#ifdef BUILD_LK
	dprintf(CRITICAL, "which_lcd_module_triple,lcd_id:%d\n", lcd_id);
#else
	LCD_DEBUG("which_lcd_module_triple,lcd_id:%d\n", lcd_id);
#endif

	lcd_id_pins_value = lcd_id;
	return lcd_id;
}
#endif
