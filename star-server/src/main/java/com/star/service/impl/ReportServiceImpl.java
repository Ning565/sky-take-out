package com.star.service.impl;

import com.star.dto.GoodsSalesDTO;
import com.star.entity.Orders;
import com.star.mapper.OrderMapper;
import com.star.mapper.UserMapper;
import com.star.service.ReportService;
import com.star.service.WorkspaceService;
import com.star.vo.*;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Select;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 查询营业额，需要查询订单已经完成的
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 统计DateList集合，存放开始到end每一天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            // 日期加一，日期依次增加到最后一天，然后添加到集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // 取出List集合元素，以,分割，拼成字符串
        String dateStr = StringUtils.join(dateList,",");

        // 查询每日营业额数据并且封装到VO集合中
        List<BigDecimal> turnoverList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            // 查询每日已完成订单对应的营业额合计
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN); //获取当天00点的时间
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            // 传入beginTime ,endTime 和 status查询，封装到Map中
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            BigDecimal turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover == null ? new BigDecimal(0) : turnover); // 如果当天没有营业额就赋值为0
        }
        // 封装结果
        String turnoverStr = StringUtils.join(turnoverList,",");
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder().
                dateList(dateStr).
                turnoverList(turnoverStr).
                build();

        return turnoverReportVO;
    }

    /**
     * 统计用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 日期同上个方法
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            // 日期加一，日期依次增加到最后一天，然后添加到集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        List<Integer> newUserList = new ArrayList<>(); // 每日新增用户数量
        List<Integer> totalUserList = new ArrayList<>();// 每日总用户数量

        // 统计用户数量，根据注册时间create_time
        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            // 先传入endTime(无begin) 查出总数量
            Map map = new HashMap();
            map.put("end",endTime);
            // 统计总数量
            totalUserList.add(userMapper.countByMap(map));
            map.put("begin",beginTime);
            // 统计新增数量
            newUserList.add(userMapper.countByMap(map));
        }
        // 取出各个List集合元素，以,分割，拼成字符串
        String dateStr = StringUtils.join(dateList,",");
        String newUserStr = StringUtils.join(newUserList,",");
        String totalUserStr = StringUtils.join(totalUserList,",");
        UserReportVO userReportVO = UserReportVO.builder().dateList(dateStr).totalUserList(totalUserStr).newUserList(newUserStr).build();
        return userReportVO;
    }

    /**
     * 统计订单完成率数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 日期列表的创建
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            // 日期加一，日期依次增加到最后一天，然后添加到集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> totalOrderList = new ArrayList<>(); // 每日订单数量
        List<Integer> validOrderList = new ArrayList<>();// 每日有效订单数量
        Integer sum = Integer.valueOf(0);
        Integer validSum = Integer.valueOf(0);
        Double orderCompletionRate = Double.valueOf(0.0);
        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            // 先查出总数量和有效订单数量，区分是订单状态
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            // 查询所有状态的订单数量
            Integer totalOrderCount = orderMapper.countByMap(map);
            // 后添加状态为完成，统计完成状态的数量
            map.put("status",Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(map);
            totalOrderList.add(totalOrderCount);
            validOrderList.add(validOrderCount);

           sum = sum + totalOrderCount;
           validSum = validSum +  validOrderCount;
        }
        String dateStr = StringUtils.join(dateList,",");
        String totalOrderCountStr = StringUtils.join(totalOrderList,",");
        String validOrderCountStr = StringUtils.join(validOrderList,",");
        if (sum != 0 ) {
            orderCompletionRate = validSum.doubleValue() / sum;
        }
        OrderReportVO orderReportVO = OrderReportVO.builder().
                dateList(dateStr).orderCountList(totalOrderCountStr).validOrderCountList(validOrderCountStr).totalOrderCount(sum).validOrderCount(validSum)
                .orderCompletionRate(orderCompletionRate).build();
        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
         // 获得商品名称和销量对象的List列表
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        // 方便后续的流式操作
        Stream<GoodsSalesDTO> stream = salesTop10.stream();
        // 使用 map 方法将每个 GoodsSalesDTO 对象转换为其名称
        Stream<String> nameString = stream.map(GoodsSalesDTO::getName);
        // 使用 collect 方法将流中的名称收集到一个 List<String> 中
        List<String> nameList = nameString.collect(Collectors.toList());
        // 销量数据同理
        Stream<GoodsSalesDTO> stream1 = salesTop10.stream();
        Stream<Integer> numString = stream1.map(GoodsSalesDTO::getNumber);
        List<Integer> numList = numString.collect(Collectors.toList());
        // 将List集合转换为String对象
        String nameStr = StringUtils.join(nameList,",");
        String numStr = StringUtils.join(numList,",");
        SalesTop10ReportVO salesTop10ReportVO = SalesTop10ReportVO.builder().nameList(nameStr).numberList(numStr).build();
        return salesTop10ReportVO;
    }

    /**
     * 导出Excel报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
            LocalDate begin = LocalDate.now().minusDays(30);
            LocalDate end = LocalDate.now().minusDays(1);
            //查询概览运营数据，提供给Excel模板文件，得到开始结束时间的业务数据
            BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN),
                            LocalDateTime.of(end, LocalTime.MAX));
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
            try { //基于提供好的模板文件创建一个新的Excel表格对象
                XSSFWorkbook excel = new XSSFWorkbook(inputStream);
                //获得Excel文件中的一个Sheet页
                XSSFSheet sheet = excel.getSheet("Sheet1");
                // 设置标题下首行为日期
                sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
                //获得第4行
                XSSFRow row = sheet.getRow(3);
                //获取单元格
                // 分别是营业额（第2列），订单完成率（第4列）和新增用户数目（第6列）,注意营业额需要转为String类型，防止丢失精度
                row.getCell(2).setCellValue(businessData.getTurnover().toString());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(6).setCellValue(businessData.getNewUsers());
                // 再次获取，获取第5行
                row = sheet.getRow(4);
                // 分别为有效订单和平均客单价
                row.getCell(2).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getUnitPrice().toString());
                for (int i = 0; i < 30; i++) {
                    // 从begin日期开始，依次增加30天，获取他们的数据并填充
                    LocalDate date = begin.plusDays(i);
                    businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                    row = sheet.getRow(7 + i);
                    // 开始设置每日数据，依次填充6列分别为日期、营业额、有效订单、订单完成率、平均客单价、新增用户
                    row.getCell(1).setCellValue(date.toString());
                    row.getCell(2).setCellValue(businessData.getTurnover().toString());
                    row.getCell(3).setCellValue(businessData.getValidOrderCount());
                    row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                    row.getCell(5).setCellValue(businessData.getUnitPrice().toString());
                    row.getCell(6).setCellValue(businessData.getNewUsers());
                }
                //通过输出流将文件下载到客户端浏览器中
                ServletOutputStream out = response.getOutputStream();
                excel.write(out);
                //关闭资源
                out.flush();
                out.close();
                excel.close();
        } catch (IOException e) {
                e.printStackTrace();
            }

    }
}
