/*
 * Copyright (C) 2023 杭州白书科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.playedu.api.controller.frontend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import xyz.playedu.common.domain.Department;
import xyz.playedu.common.service.DepartmentService;
import xyz.playedu.common.types.JsonResponse;

import java.util.stream.Collectors;

/**
 * @Author 杭州白书科技有限公司
 *
 * @create 2023/3/13 16:23
 */
@RestController
@RequestMapping("/api/v1/department")
public class DepartmentController {

    @Autowired private DepartmentService departmentService;

    @GetMapping("/index")
    public JsonResponse index() {
        return JsonResponse.data(
                departmentService.all().stream()
                        .collect(Collectors.groupingBy(Department::getParentId)));
    }
}
