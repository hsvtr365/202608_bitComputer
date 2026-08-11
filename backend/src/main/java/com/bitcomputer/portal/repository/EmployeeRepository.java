package com.bitcomputer.portal.repository;

import com.bitcomputer.portal.domain.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmployeeNumber(String employeeNumber);
    List<Employee> findAllByOrderByEmployeeNumberAsc();
    List<Employee> findByNameContainingIgnoreCaseOrEmployeeNumberContainingIgnoreCaseOrderByEmployeeNumberAsc(
            String name, String employeeNumber);
    @Query("select distinct e.department from Employee e order by e.department")
    List<String> findDistinctDepartments();
    @Query("select distinct e.position from Employee e order by e.position")
    List<String> findDistinctPositions();
}
