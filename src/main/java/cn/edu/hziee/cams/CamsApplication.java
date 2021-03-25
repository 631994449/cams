package cn.edu.hziee.cams;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.activiti.spring.boot.SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@MapperScan("cn.edu.hziee.cams.mapper") //扫描的mapper
public class CamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamsApplication.class, args);
    }

}
