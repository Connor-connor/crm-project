package com.lihaopeng.crm.settings.web.controller;

import com.lihaopeng.crm.commons.constants.Constants;
import com.lihaopeng.crm.commons.domain.ReturnObject;
import com.lihaopeng.crm.commons.utils.DateUtils;
import com.lihaopeng.crm.settings.domain.User;
import com.lihaopeng.crm.settings.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Connor
 * @date 2022/10/11 16:29
 */
// 别忘了在mvc添加包扫描
@Controller
public class UserController {

    // 一定要注入service层的对象
    @Autowired
    private UserService userService;

    // TODO: 004 请求转发到 /settings/qx/user/ 目录(由@RequestMapping所示)下的 login 页面(由return所示) [sd首页9]
    // 这个url要和controller方法处理完请求之后,响应信息返回的页面的资源目录保持一致(这里是src/main/webapp/WEB-INF/pages/settings/qx/user/login.jsp)
    // 最后的资源名称要和方法名保持一致,同时一般习惯加一个.do,配置核心控制器的时候把.do资源也交给它处理
    // @RequestMapping("/WEB-INF/pages/settings/qx/user/toLogin.do")
    // 又发现,将来所有的controller不论返回到那个页面,页面都在/WEB-INF/pages下,所以也可以省略不写
    @RequestMapping("/settings/qx/user/toLogin.do")
    public String toLogin() {
        // 请求转发到登录页面
        // 记住,视图解析器里已经配了前缀后缀,所以这里settings前面不需要加/了
        return "settings/qx/user/login";
    }

    // TODO: 006 接收到登录请求,获取参数并封装,调用service层方法,查询用户 [sd用户登录4/5/6]
    // 将来我们要通过这个返回值返回响应一个json对象{code:1|0,message:xxx},就要把这个返回值封装成一个java对象(才能调用返回json对象)
    // 那么返回什么类型的对象呢?如果返回String的话得自己转成json对象,肯定不行;如果用User类的话,User类里面也没有code和message对象,也不行
    // 所以我们要自己创建一个java类,专门封装这种要返回json数据的对象;再想,我们现在只需要返回code和message这两条数据,如果以后要返回的增加了呢?
    // 所以我们要返回一个更加通用的Object,将来不管返回什么类型的java对象都可以,想返回什么样的json都可以,这就是多态
    // 要返回一个json对象,就要加一个@ResponseBody,写在外面也是一样的
    // @ResponseBody
    @RequestMapping("/settings/qx/user/login.do")
    public @ResponseBody Object login(String loginAct, String loginPwd, String isRemPwd,
                                      HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        // 封装参数
        // 封装的key起的名要和(UserMapper.xml中)数据库sql语句用的#{loginPwd}保持一致
        Map<String, Object> map = new HashMap<>();
        map.put("loginAct", loginAct);
        map.put("loginPwd", loginPwd);
        // 调用service层方法,查询用户
        User user = userService.queryUserByLoginActAndPwd(map);

        // TODO: 009 根据查询后返回的user,生成响应信息,并响应回去 [sd用户登录11/12]
        // 根据查询结果,生成响应信息
        ReturnObject returnObject = new ReturnObject();
        if (user == null) {
            // 登录失败,用户名或者密码错误
            returnObject.setCode(Constants.RETURN_OBJECT_CODE_FAIL);
            returnObject.setMessage("用户名或者密码错误");
        } else {
            // 可以把user.getExpireTime()这个字符串转成Date,可以自己试一试
            // 这里采取把Date转成字符串
            if (DateUtils.formatDataTime(new Date()).compareTo(user.getExpireTime()) > 0) {
                // 登录失败,账号已过期
                returnObject.setCode(Constants.RETURN_OBJECT_CODE_FAIL);
                returnObject.setMessage("账号已过期");
            } else if ("0".equals(user.getLockState())){
                // 登录失败,状态被锁定
                returnObject.setCode(Constants.RETURN_OBJECT_CODE_FAIL);
                returnObject.setMessage("状态被锁定");
            } else if (!user.getAllowIps().contains(request.getRemoteAddr())) { //获取远程的用户的ip
                // 登录失败,ip受限
                returnObject.setCode(Constants.RETURN_OBJECT_CODE_FAIL);
                returnObject.setMessage("ip受限");
            }else{
                // 登录成功
                returnObject.setCode(Constants.RETURN_OBJECT_CODE_SUCCESS);

                // 以后,不论是在什么层的Controller里面,想要把数据传给页面,都要通过作用域
                // 即,把控制层(Controller)代码中处理好的数据传递到视图层(jsp),使用作用域传递:
                // pageContext: 用来在同一个页面的不同标签之间传递数据,例如,在jsp中自定义标签时会用到
                // request: 在同一个请求过程中传递数据
                // session: 在同一个浏览器窗口的不同请求之间传递数据
                // application: 服务器开启的时候,所有用户都共享的数据,并且长久频繁使用的数据

                // 把user保存在session中,首先要在方法形参中注入HttpSession对象
                session.setAttribute(Constants.SESSION_USER,user);

                // 如果需要记住密码实现10天免登录,则往外写cookie
                if("true".equals(isRemPwd)){
                    Cookie c1 = new Cookie("loginAct",user.getLoginAct());
                    c1.setMaxAge(10*24*60*60);
                    response.addCookie(c1);
                    Cookie c2 = new Cookie("loginPwd",user.getLoginPwd());
                    c2.setMaxAge(10*24*60*60);
                    response.addCookie(c2);
                }else{
                    // 把没有过期的cookie删除
                    Cookie c1 = new Cookie("loginAct","1");
                    c1.setMaxAge(0);
                    response.addCookie(c1);
                    Cookie c2 = new Cookie("loginPwd","1");
                    c2.setMaxAge(0);
                    response.addCookie(c2);
                }
            }
        }
        // 因为前面已经加了@ResponseBody,会自动帮我们转换成json
        return returnObject;
    }

    @RequestMapping("/settings/qx/user/logout.do")
    public String logout(HttpServletResponse response,HttpSession session){
        // 清空cookie
        Cookie c1 = new Cookie("loginAct","1");
        c1.setMaxAge(0);
        response.addCookie(c1);
        Cookie c2 = new Cookie("loginPwd","1");
        c2.setMaxAge(0);
        response.addCookie(c2);
        // 销毁session
        session.invalidate();
        // 跳转到首页(使用重定向,否则地址栏显示的是controller的地址而不是index地址,当点击刷新会不断循环)
        return "redirect:/"; // 借助springmvc来重定向,将这行代码翻译成response.sendRedirect("/crm/");

    }
}
