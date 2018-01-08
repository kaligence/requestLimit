package com.github.kaligence.requestLimit;

import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 17.10.31
 * 完成核心的限制接口单位时间访问次数的功能，基于redis的key-value的过期时间
 * ***************************************************************************
 * 现在的问题    逻辑2选1   <目前实现的是1>
 * 1.自特定ip第一次访问接口开始计时，之后的单位时间内次数超过限制次数即不执行该接口要做的操作
 * 2.同1，然后超过限制次数之后的每一次访问都重置redis中key-value的过期时间
 * ***************************************************************************
 * 17.11.1
 * 实现了 在未达到访问次数限制之前 返回的是该接口原本的返回值
 * 在达到访问次数限制之后 返回的是自定义注解中定义的返回值
 * 基于接口返回值为JSONObject
 */
@Aspect
@Component
public class RequestInterceptor {

    @Resource(name = "jedisPool_web")
    private JedisPool jedisPool;

    private static Logger logger = Logger.getLogger(RequestInterceptor.class);

//	private static Map<String, Integer> map = new HashMap<String, Integer>();
//	private static int count = 0;

    @Pointcut("@annotation(com.github.kaligence.requestLimit.RequestLimit)")
    public void controllerAspect() {
    }

    // @Before("controllerAspect()")
    // public void doBefore(ProceedingJoinPoint proceedingjoinPoint) throws
    // Exception {
    // System.out.println("<<<<<SysLogAspect前置通知开始=====");
    // requestLimit(proceedingjoinPoint);
    // }

    @Around("controllerAspect()")
    public JSONObject doAround(ProceedingJoinPoint proceedingjoinPoint) throws Exception {
        System.out.println(">>>>>SysLogAspect环绕通知开始=====");
        return requestLimit(proceedingjoinPoint);
    }

    @After("controllerAspect()")
    public void doAfter() throws Exception {
        System.out.println("=====SysLogAspect后置通知开始<<<<<");
        // requestLimit(joinPoint);
    }

    // @AfterThrowing(value = "controllerAspect()", throwing = "e")
    // public void doAfter(JoinPoint joinPoint, Exception e) throws Exception {
    // System.out.println("=====SysLogAspect异常通知开始=====");
    // // requestLimit(joinPoint);
    // }

    private static RequestLimit giveController(ProceedingJoinPoint proceedingjoinPoint) throws Exception {
        Signature signature = proceedingjoinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null) {
            // System.out.println(">>>method不为空");
            return method.getAnnotation(RequestLimit.class);
        }
        return null;
    }


    // @Before("within(@org.springframework.stereotype.Controller *) &&
    // @annotation(limit)")
    // @Before("@annotation(com.ice.common.interceptor.RequestLimit)")
    public JSONObject requestLimit(ProceedingJoinPoint proceedingJoinPoint) throws Exception {
        JSONObject jsonObj = new JSONObject();
        RequestLimit limit = giveController(proceedingJoinPoint);

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            if (request == null) {
                logger.error("方法中缺失HttpServletRequest参数");
            }
            String ip = request.getLocalAddr();/*NetworkUtil.getIpAddress(request)*/
//			String ip = getRemortIP(request);
//			String ip = NetworkUtil.getIpAddress(request);
            String url = request.getRequestURL().toString();
            // String key = "req_limit_".concat(url).concat(ip);

            // map.put("time", System.currentTimeMillis());
//			map.put(ip, ++count);
            // logger.info(">>>"+map.get("time"));
//			logger.info(">>>" + ip + " >>> " + map.get(ip));

            Jedis jedis = jedisPool.getResource();

//			jedis.set(ip, "1", "XX", "EX", limit.requestTime());
            if ((jedis.get(ip) == null) || ("".equals(jedis.get(ip)))) {
                jedis.set(ip, "1", "NX", "EX", limit.requestTime());
            } else {
                jedis.incrBy(ip, 1);
            }

//			jedis.set(ip, "0");
//			long count = jedis.incrBy(ip, 1);
            logger.info(">>>" + jedis.get(ip));
//			if (count == 1) {
//				jedis.expireAt(ip, limit.requestTime());
//            }
            if (Integer.valueOf(jedis.get(ip)) > limit.requestCount()) {
                logger.info(">>>用户IP[" + ip + "]访问地址[" + url + "]超过了限定的次数[" + limit.requestCount() + "]");
//              jedis.expireAt(ip, limit.requestTime()*1000);
                logger.info(">>>key剩余时间：" + jedis.ttl(ip));
                jsonObj.put("limit", true);
            } else {
                jsonObj = (JSONObject) proceedingJoinPoint.proceed();
            }

            // if (System.currentTimeMillis() - (long) map.get("time") <= 1000 *
            // 60) {
            // logger.info(">>>一分钟之内");
            // if (map.get(ip) > limit.requestCount()) {
            // logger.info(">>>用户IP[" + ip + "]访问地址[" + url + "]超过了限定的次数[" +
            // limit.requestCount() + "]");
            // }
            // } else {
            // // 重置访问次数
            // map.put(ip, 1L);
            // }


//			task_resetMap(limit.requestTime());
//			if (map.get(ip) > limit.requestCount()) {
//				logger.info(">>>用户IP[" + ip + "]访问地址[" + url + "]超过了限定的次数[" + limit.requestCount() + "]");
//			} else {
//				proceedingJoinPoint.proceed();
//			}

        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            jedisPool.close();
        }
        return jsonObj;
    }

//	/**
//	 * resetMap定时任务
//	 */
//	private void task_resetMap(long timeLimit) {
//		Runnable runnable = new Runnable() {
//			public void run() {
//				try {
//					resetMap();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		};
//		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//		service.scheduleAtFixedRate(runnable, timeLimit, timeLimit, TimeUnit.SECONDS);// 7000s后开始定时任务，每7000s执行一次
//	}
//
//	protected void resetMap() {
//		// 重置访问次数
//		map.put(ip, 0);
//		count = 0;
//	}
}
