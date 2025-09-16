package com.star.service;

import com.star.dto.EmployeeDTO;
import com.star.dto.EmployeeLoginDTO;
import com.star.dto.EmployeePageQueryDTO;
import com.star.entity.Employee;
import com.star.result.PageResult;
import com.star.vo.EmployeeVO;

public interface EmployeeService {

    Employee login(EmployeeLoginDTO employeeLoginDTO);

    void save(EmployeeDTO employeeDTO);

    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    void startOrStop(Integer status, Long id);

    EmployeeVO getById(Long id);

    void update(EmployeeDTO employeeDTO);
}
