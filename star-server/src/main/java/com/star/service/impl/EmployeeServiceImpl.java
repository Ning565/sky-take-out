package com.star.service.impl;

import com.alibaba.druid.support.spring.stat.annotation.Stat;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.star.constant.MessageConstant;
import com.star.constant.PasswordConstant;
import com.star.constant.StatusConstant;
import com.star.context.BaseContext;
import com.star.dto.EmployeeDTO;
import com.star.dto.EmployeeLoginDTO;
import com.star.dto.EmployeePageQueryDTO;
import com.star.entity.Employee;
import com.star.exception.AccountLockedException;
import com.star.exception.AccountNotFoundException;
import com.star.exception.PasswordErrorException;
import com.star.mapper.EmployeeMapper;
import com.star.result.PageResult;
import com.star.service.EmployeeService;
import com.star.vo.EmployeeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // md5加密比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        // 1. 对象属性拷贝，empDTO沟通前后端，尽量少，emp给数据库看的
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);// BeanUtils copy方法：对象的相同名称属性值复制到另一个对象
        // 2.补充基本信息，账号状态，默认密码，创建时间修改时间，创建人/修改人ID
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        // 获取当前用户ID作为创建人/修改人ID , 通过jwt令牌获取，利用ThreadLocal取值，同一次请求是一个线程，共享存储资源

        // 3.Mapper层实现
        employeeMapper.insert(employee);
    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 1.处理起始页和每页数量
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        // 2.返回类型必须为Page<Emp>，获取的page和size通过线程池传递 ,调用Mapper层接口查询
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        // 3.获取返回结果，封装到PageResult中
        long total = page.getTotal();
        List<Employee> records = page.getResult();
        return new PageResult(total,records);
    }

    /**
     * 编辑员工当前状态
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // builder()注解构建器，构建器对象.status，不同编程风格
        Employee employee = Employee.builder().
                            status(status).id(id).build();
        employeeMapper.update(employee);
    }

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Override
    public EmployeeVO getById(Long id) {

        EmployeeVO employeeVO = new EmployeeVO();
        BeanUtils.copyProperties( employeeMapper.getById(id),employeeVO);
        return employeeVO;
    }

    /**
     * 更新员工信息
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        // 1.DTO类型转化为实体
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
        // 2.补充基本信息

        employeeMapper.update(employee);
    }

}
