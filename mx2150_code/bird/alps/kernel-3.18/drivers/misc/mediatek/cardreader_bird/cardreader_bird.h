#ifndef __CARDREADER_H__
#define __CARDREADER_H__

#define CARDREADER_DEBUG

#if defined(CARDREADER_DEBUG)
#define CARDREADER_TAG		"[CARDREADER] "
#define CARDREADER_FUN(f)		printk(CARDREADER_TAG"%s\n", __FUNCTION__)
#define CARDREADER_ERR(fmt, args...)	printk(CARDREADER_TAG"%s %d : "fmt, __FUNCTION__, __LINE__, ##args)
#define CARDREADER_LOG(fmt, args...)	printk(CARDREADER_TAG fmt, ##args)
#define CARDREADER_DBG(fmt, args...)	printk(CARDREADER_TAG fmt, ##args)    
#else
#define CARDREADER_FUN(f)
#define CARDREADER_ERR(fmt, args...)
#define CARDREADER_LOG(fmt, args...)
#define CARDREADER_DBG(fmt, args...)
#endif

#endif
