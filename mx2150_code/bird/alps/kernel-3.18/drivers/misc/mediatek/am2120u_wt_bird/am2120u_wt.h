#ifndef __AM2120U_WT_H__
#define __AM2120U_WT_H__

#define AM2120U_WT_DEBUG

#if defined(AM2120U_WT_DEBUG)
#define AM2120U_WT_TAG		"[AM2120U_WT] "
#define AM2120U_WT_FUN(f)		printk(AM2120U_WT_TAG"%s\n", __FUNCTION__)
#define AM2120U_WT_ERR(fmt, args...)	printk(AM2120U_WT_TAG"%s %d : "fmt, __FUNCTION__, __LINE__, ##args)
#define AM2120U_WT_LOG(fmt, args...)	printk(AM2120U_WT_TAG fmt, ##args)
#define AM2120U_WT_DBG(fmt, args...)	printk(AM2120U_WT_TAG fmt, ##args)    
#else
#define AM2120U_WT_FUN(f)
#define AM2120U_WT_ERR(fmt, args...)
#define AM2120U_WT_LOG(fmt, args...)
#define AM2120U_WT_DBG(fmt, args...)
#endif

#endif
