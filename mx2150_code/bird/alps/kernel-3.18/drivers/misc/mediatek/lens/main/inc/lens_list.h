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

#ifndef _LENS_LIST_H

#define _LENS_LIST_H

#ifdef CONFIG_MTK_LENS_AK7371AF_SUPPORT
#define AK7371AF_SetI2Cclient AK7371AF_SetI2Cclient_Main
#define AK7371AF_Ioctl AK7371AF_Ioctl_Main
#define AK7371AF_Release AK7371AF_Release_Main
extern void AK7371AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long AK7371AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int AK7371AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
extern int AK7371AF_PowerDown(void);
#endif

#ifdef CONFIG_MTK_LENS_BU6424AF_SUPPORT
#define BU6424AF_SetI2Cclient BU6424AF_SetI2Cclient_Main
#define BU6424AF_Ioctl BU6424AF_Ioctl_Main
#define BU6424AF_Release BU6424AF_Release_Main
extern void BU6424AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long BU6424AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int BU6424AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_BU6429AF_SUPPORT
#define BU6429AF_SetI2Cclient BU6429AF_SetI2Cclient_Main
#define BU6429AF_Ioctl BU6429AF_Ioctl_Main
#define BU6429AF_Release BU6429AF_Release_Main
extern void BU6429AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long BU6429AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int BU6429AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_BU63165AF_SUPPORT
#define BU63165AF_SetI2Cclient BU63165AF_SetI2Cclient_Main
#define BU63165AF_Ioctl BU63165AF_Ioctl_Main
#define BU63165AF_Release BU63165AF_Release_Main
extern void BU63165AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				   int *pAF_Opened);
extern long BU63165AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			    unsigned long a_u4Param);
extern int BU63165AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_DW9714AF_SUPPORT
#define DW9714AF_SetI2Cclient DW9714AF_SetI2Cclient_Main
#define DW9714AF_Ioctl DW9714AF_Ioctl_Main
#define DW9714AF_Release DW9714AF_Release_Main
extern void DW9714AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long DW9714AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int DW9714AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_DW9814AF_SUPPORT
#define DW9814AF_SetI2Cclient DW9814AF_SetI2Cclient_Main
#define DW9814AF_Ioctl DW9814AF_Ioctl_Main
#define DW9814AF_Release DW9814AF_Release_Main
extern void DW9814AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long DW9814AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int DW9814AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_DW9718AF_SUPPORT
#define DW9718AF_SetI2Cclient DW9718AF_SetI2Cclient_Main
#define DW9718AF_Ioctl DW9718AF_Ioctl_Main
#define DW9718AF_Release DW9718AF_Release_Main
extern void DW9718AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long DW9718AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int DW9718AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_DW9719TAF_SUPPORT
#define DW9719TAF_SetI2Cclient DW9719TAF_SetI2Cclient_Main
#define DW9719TAF_Ioctl DW9719TAF_Ioctl_Main
#define DW9719TAF_Release DW9719TAF_Release_Main
extern void DW9719TAF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				   int *pAF_Opened);
extern long DW9719TAF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			    unsigned long a_u4Param);
extern int DW9719TAF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_FM50AF_SUPPORT
#define FM50AF_SetI2Cclient FM50AF_SetI2Cclient_Main
#define FM50AF_Ioctl FM50AF_Ioctl_Main
#define FM50AF_Release FM50AF_Release_Main
extern void FM50AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				int *pAF_Opened);
extern long FM50AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int FM50AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_LC898122AF_SUPPORT
#define LC898122AF_SetI2Cclient LC898122AF_SetI2Cclient_Main
#define LC898122AF_Ioctl LC898122AF_Ioctl_Main
#define LC898122AF_Release LC898122AF_Release_Main
extern void LC898122AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				    int *pAF_Opened);
extern long LC898122AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			     unsigned long a_u4Param);
extern int LC898122AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_LC898212AF_SUPPORT
#define LC898212AF_SetI2Cclient LC898212AF_SetI2Cclient_Main
#define LC898212AF_Ioctl LC898212AF_Ioctl_Main
#define LC898212AF_Release LC898212AF_Release_Main
extern void LC898212AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				    int *pAF_Opened);
extern long LC898212AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			     unsigned long a_u4Param);
extern int LC898212AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_LC898212XDAF_SUPPORT
#define LC898212XDAF_SetI2Cclient LC898212XDAF_SetI2Cclient_Main
#define LC898212XDAF_Ioctl LC898212XDAF_Ioctl_Main
#define LC898212XDAF_Release LC898212XDAF_Release_Main
extern void LC898212XDAF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				      int *pAF_Opened);
extern long LC898212XDAF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			       unsigned long a_u4Param);
extern int LC898212XDAF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_LC898214AF_SUPPORT
#define LC898214AF_SetI2Cclient LC898214AF_SetI2Cclient_Main
#define LC898214AF_Ioctl LC898214AF_Ioctl_Main
#define LC898214AF_Release LC898214AF_Release_Main
extern void LC898214AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				    int *pAF_Opened);
extern long LC898214AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			     unsigned long a_u4Param);
extern int LC898214AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_AD5820AF_SUPPORT
#define AD5820AF_SetI2Cclient AD5820AF_SetI2Cclient_Main
#define AD5820AF_Ioctl AD5820AF_Ioctl_Main
#define AD5820AF_Release AD5820AF_Release_Main
extern void AD5820AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long AD5820AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int AD5820AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_WV511AAF_SUPPORT
#define WV511AAF_SetI2Cclient WV511AAF_SetI2Cclient_Main
#define WV511AAF_Ioctl WV511AAF_Ioctl_Main
#define WV511AAF_Release WV511AAF_Release_Main
extern void WV511AAF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long WV511AAF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int WV511AAF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

//BIRD BEGIN
#ifdef CONFIG_MTK_LENS_OV5695AF_SUPPORT
#define OV5695AF_SetI2Cclient OV5695AF_SetI2Cclient_Main
#define OV5695AF_Ioctl OV5695AF_Ioctl_Main
#define OV5695AF_Release OV5695AF_Release_Main
extern void OV5695AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long OV5695AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int OV5695AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_OV8856AF_SUPPORT
#define OV8856AF_SetI2Cclient OV8856AF_SetI2Cclient_Main
#define OV8856AF_Ioctl OV8856AF_Ioctl_Main
#define OV8856AF_Release OV8856AF_Release_Main
extern void OV8856AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long OV8856AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int OV8856AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_GC8034AF_SUPPORT
extern void GC8034AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long GC8034AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int GC8034AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_GC8024AF_SUPPORT
#define GC8024AF_SetI2Cclient GC8024AF_SetI2Cclient_Main
#define GC8024AF_Ioctl GC8024AF_Ioctl_Main
#define GC8024AF_Release GC8024AF_Release_Main
extern void GC8024AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long GC8024AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int GC8024AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_GC5024AF_SUPPORT
#define GC5024AF_SetI2Cclient GC5024AF_SetI2Cclient_Main
#define GC5024AF_Ioctl GC5024AF_Ioctl_Main
#define GC5024AF_Release GC5024AF_Release_Main
extern void GC5024AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long GC5024AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int GC5024AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_GC5005AF_SUPPORT
#define GC5005AF_SetI2Cclient GC5005AF_SetI2Cclient_Main
#define GC5005AF_Ioctl GC5005AF_Ioctl_Main
#define GC5005AF_Release GC5005AF_Release_Main
extern void GC5005AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long GC5005AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int GC5005AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_GC5025AF_SUPPORT
#define GC5025AF_SetI2Cclient GC5025AF_SetI2Cclient_Main
#define GC5025AF_Ioctl GC5025AF_Ioctl_Main
#define GC5025AF_Release GC5025AF_Release_Main
extern void GC5025AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long GC5025AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int GC5025AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_OV13850AF_SUPPORT
extern void OV13850AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long OV13850AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int OV13850AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_OV8858AF_SUPPORT
#define OV8858AF_SetI2Cclient OV8858AF_SetI2Cclient_Main
#define OV8858AF_Ioctl OV8858AF_Ioctl_Main
#define OV8858AF_Release OV8858AF_Release_Main
extern void OV8858AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long OV8858AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int OV8858AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_OV8865AF_SUPPORT
#define OV8865AF_SetI2Cclient OV8865AF_SetI2Cclient_Main
#define OV8865AF_Ioctl OV8865AF_Ioctl_Main
#define OV8865AF_Release OV8865AF_Release_Main
extern void OV8865AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long OV8865AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int OV8865AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif


#ifdef CONFIG_MTK_LENS_S5K3L8AF_SUPPORT
extern void S5K3L8AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long S5K3L8AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int S5K3L8AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_S5K4H8AF_SUPPORT
#define S5K4H8AF_SetI2Cclient S5K4H8AF_SetI2Cclient_Main
#define S5K4H8AF_Ioctl S5K4H8AF_Ioctl_Main
#define S5K4H8AF_Release S5K4H8AF_Release_Main
extern void S5K4H8AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock,
				  int *pAF_Opened);
extern long S5K4H8AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command,
			   unsigned long a_u4Param);
extern int S5K4H8AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif


#ifdef CONFIG_MTK_LENS_OV5647AF_SUPPORT
extern void OV5647AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long OV5647AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int OV5647AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_OV5647AAF_SUPPORT
extern void OV5647AAF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long OV5647AAF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int OV5647AAF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_IMX219AF_SUPPORT
extern void IMX219AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long IMX219AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int IMX219AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_IMX214AF_SUPPORT
extern void IMX214AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long IMX214AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int IMX214AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_IMX149AF_SUPPORT
extern void IMX149AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long IMX149AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int IMX149AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_IMX258AF_SUPPORT
extern void IMX258AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long IMX258AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int IMX258AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif


#ifdef CONFIG_MTK_LENS_SP5506AF_SUPPORT
extern void SP5506AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long SP5506AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int SP5506AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_S5K5E2YAAF_SUPPORT
extern void S5K5E2YAAF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long S5K5E2YAAF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int S5K5E2YAAF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_S5K3M2AF_SUPPORT
extern void S5K3M2AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long S5K3M2AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int S5K3M2AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_BF2507AF_SUPPORT
extern void BF2507AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long BF2507AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int BF2507AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

#ifdef CONFIG_MTK_LENS_AR0543AF_SUPPORT
extern void AR0543AF_SetI2Cclient(struct i2c_client *pstAF_I2Cclient, spinlock_t *pAF_SpinLock, int *pAF_Opened);
extern long AR0543AF_Ioctl(struct file *a_pstFile, unsigned int a_u4Command, unsigned long a_u4Param);
extern int AR0543AF_Release(struct inode *a_pstInode, struct file *a_pstFile);
#endif

//BIRD END

#endif
