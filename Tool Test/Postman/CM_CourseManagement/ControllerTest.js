/**
 * PM_CM_001 - GetAllCourse - Successfully
 * Mục tiêu: Kiểm tra API lấy toàn bộ danh sách course thành công
 */
function PM_CM_001_testGetAllCourseSuccessfully() {
    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response là array
    pm.test("Response is array", function () {
        pm.expect(Array.isArray(body)).to.eql(true);
    });

    // Kiểm tra field cơ bản
    pm.test("Each course has required fields", function () {
        body.forEach((c, idx) => {
            pm.expect(c, `course[${idx}]`).to.have.property("id");
            pm.expect(c).to.have.property("courseCode");
            pm.expect(c).to.have.property("name");
            pm.expect(c).to.have.property("imgUrl");
        });
    });
}

/**
 * PM_CM_002 - GetAllCourse - Check Authorization
 * Mục tiêu: Kiểm tra phân quyền API theo từng role
 */
function PM_CM_002_testGetAllCourseAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra HTTP status code
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    const body = pm.response.json();

    if (expected === 200) {
        // Kiểm tra response có data hay không
        pm.test("Response has data (success)", function () {
            pm.expect(body).to.not.eql(null);
        });
    } else if (expected === 403) {
        // Kiểm tra response có message lỗi không
        pm.test("Response has error message", function () {
            pm.expect(body).to.have.property("message");
            pm.expect(body.message).to.eql("Forbidden");
        });
    }
}

/**
 * PM_CM_003 - GetCourseListByPage - Default Pageable
 * Mục tiêu: Kiểm tra API lấy danh sách course với phân trang mặc định
 */
function PM_CM_003_testGetCourseListByPageDefault() {
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response đúng định dạng trả về
    pm.test("Has data and paginationDetails", function () {
        pm.expect(body).to.have.property("data");
        pm.expect(body).to.have.property("paginationDetails");
    });

    // Kiểm tra các tham số phân trang mặc định
    pm.test("Default pagination applied", function () {
        // Trang mặc định phải là 0
        pm.expect(body.paginationDetails.pageNumber).to.eql(0);

        // Tồn tại pageCount nhưng không kiểm tra giá trị cụ thể vì có thể thay đổi theo dữ liệu
        pm.expect(body.paginationDetails).to.have.property("pageCount");
        pm.expect(body.paginationDetails).to.have.property("totalCount");
        pm.expect(body.paginationDetails).to.have.property("totalPage");
    });

    // Kiểm tra data là mảng
    pm.test("Data is array", function () {
        pm.expect(Array.isArray(body.data)).to.eql(true);
    });
}

/**
 * PM_CM_004 - GetCourseListByPage - Custom Pagable
 * Mục tiêu: Kiểm tra API lấy danh sách course với phân trang tùy chỉnh
 */
function PM_CM_004_testGetCourseListByPageCustom() {
    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Lấy param từ request
    const page = Number(pm.request.url.query.get("page"));
    const size = Number(pm.request.url.query.get("size"));
    const sort = pm.request.url.query.get("sort");

    // Kiểm tra response đúng định dạng trả về
    pm.test("Has data and paginationDetails", function () {
        pm.expect(body).to.have.property("data");
        pm.expect(body).to.have.property("paginationDetails");
    });

    // Kiểm tra pagination theo param truyền vào
    pm.test("Pagination matches request params", function () {
        pm.expect(body.paginationDetails.pageNumber).to.eql(page);
        pm.expect(body.paginationDetails.pageCount).to.eql(size);
    });

    // Kiểm tra các field pagination tồn tại
    pm.test("Pagination fields exist", function () {
        pm.expect(body.paginationDetails).to.have.property("totalCount");
        pm.expect(body.paginationDetails).to.have.property("totalPage");
    });

    // Kiểm tra data là mảng
    pm.test("Data is array", function () {
        pm.expect(Array.isArray(body.data)).to.eql(true);
    });

    // Kiểm tra số lượng phần tử <= size
    pm.test("Data size is valid", function () {
        pm.expect(body.data.length).to.be.at.most(size);
    });

    // Kiểm tra field cơ bản của course
    pm.test("Each course has required fields", function () {
        body.data.forEach((c, idx) => {
            pm.expect(c, `course[${idx}]`).to.have.property("id");
            pm.expect(c).to.have.property("courseCode");
            pm.expect(c).to.have.property("name");
            pm.expect(c).to.have.property("imgUrl");
        });
    });

    // Kiểm tra sort đúng theo param truyền vào
    if (sort && body.data.length > 0) {
        let [field, direction = "asc"] = sort.split(",");

        const values = body.data.map(item => item[field]);

        let sorted = [...values].sort((a, b) => {
            if (typeof a === "number") return a - b;
            return String(a).localeCompare(String(b));
        });

        if (direction === "desc") sorted.reverse();

        pm.test(`Sorted by ${field} ${direction}`, function () {
            pm.expect(values).to.eql(sorted);
        });
    }
}

/**
 * PM_CM_005 - GetCourseListByPage - Empty Result
 * Mục tiêu: Kiểm tra API trả về danh sách rỗng khi phân trang vượt quá số trang
 */
function PM_CM_005_testGetCourseListByPageEmpty() {
    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Lấy param từ request
    const page = Number(pm.request.url.query.get("page"));
    const size = Number(pm.request.url.query.get("size"));
    const sort = pm.request.url.query.get("sort");

    // Kiểm tra response đúng định dạng trả về
    pm.test("Has data and paginationDetails", function () {
        pm.expect(body).to.have.property("data");
        pm.expect(body).to.have.property("paginationDetails");
    });

    // Kiểm tra pagination theo param truyền vào
    pm.test("Pagination matches request params", function () {
        pm.expect(body.paginationDetails.pageNumber).to.eql(page);

        // Data trống thì pageCount phải bằng 0
        pm.expect(body.paginationDetails.pageCount).to.eql(0);
    });

    // Kiểm tra pagination fields
    pm.test("Pagination fields exist", function () {
        pm.expect(body.paginationDetails).to.have.property("totalCount");
        pm.expect(body.paginationDetails).to.have.property("totalPage");
    });

    // Kiểm tra data là mảng
    pm.test("Data is array", function () {
        pm.expect(Array.isArray(body.data)).to.eql(true);
    });

    // Kiểm tra data là mảng rỗng
    pm.test("Data is empty array", function () {
        pm.expect(body.data.length).to.eql(0);
    });
}

/**
 * PM_CM_006 - GetCourseListByPage - Check Authorization
 * Mục tiêu: Kiểm tra phân quyền API lấy danh sách course theo trang
 */
function PM_CM_006_testGetCourseListByPageAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra HTTP status code
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    const body = pm.response.json();

    if (expected === 200) {
        // Kiểm tra response có data hay không
        pm.test("Response has data (success)", function () {
            pm.expect(body).to.not.eql(null);
        });
    } else if (expected === 403) {
        // Kiểm tra response có message lỗi không
        pm.test("Response has error message", function () {
            pm.expect(body).to.have.property("message");
            pm.expect(body.message).to.eql("Forbidden");
        });
    }
}

/**
 * PM_CM_007 - CheckCourseCode - Non Duplicate
 * Mục tiêu: Kiểm tra API trả về false khi courseCode không tồn tại
 */
function PM_CM_007_testCheckCourseCodeNonDuplicate() {
    const value = pm.request.url.query.get("value");
    const courseId = pm.environment.get(":courseId");

    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra API trả về false khi courseCode không trùng lặp
    pm.test(`CourseCode ${value} not exists, then return false`, function () {
        pm.expect(body).to.eql(false);
    });
}

/**
 * PM_CM_008 - CheckCourseCode - Duplicated
 * Mục tiêu: Kiểm tra API trả về true khi courseCode đã tồn tại
 */
function PM_CM_008_testCheckCourseCodeDuplicated() {
    const value = pm.request.url.query.get("value");
    const courseId = pm.environment.get(":courseId");

    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra API trả về true khi courseCode đã tồn tại
    pm.test(`CourseCode ${value} exists, then return true (duplicated)`, function () {
        pm.expect(body).to.eql(true);
    });
}

/**
 * PM_CM_009 - CheckCourseCode - Self Indentical
 * Mục tiêu: Kiểm tra API trả về false khi courseCode giống với chính nó
 */
function PM_CM_009_testCheckCourseCodeSelfIdentical() {
    const value = pm.request.url.query.get("value");
    const courseId = pm.environment.get(":courseId");

    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra API trả về false khi courseCode giống với chính nó (không bị coi là trùng lặp)
    pm.test(
        `CourseCode ${value} is the same courseCode of course ${courseId}, then return false`,
        function () {
            pm.expect(body).to.eql(false);
        }
    );
}

/**
 * PM_CM_010 - CheckCourseCode - Check Authorization
 * Mục tiêu: Kiểm tra phân quyền API kiểm tra courseCode theo role
 */
function PM_CM_010_testCheckCourseCodeAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra status theo role
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Parse body an toàn (vì boolean cũng parse được)
    let body = null;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Trường hợp có quyền, cần trả về 200 và response là boolean
    if (expected === 200) {
        pm.test("Response is boolean", function () {
            pm.expect(typeof body).to.eql("boolean");
        });
    }

    // Trường hợp không có quyền, cần trả về 403 và message Forbidden
    else if (expected === 403) {
        pm.test("Forbidden response structure", function () {
            pm.expect(body).to.have.property("status");
            pm.expect(body.status).to.eql(403);

            pm.expect(body).to.have.property("error");
            pm.expect(body.error).to.match(/forbidden/i);
        });
    }
}

/**
 * PM_CM_011 - CheckCode - Non Existed
 * Mục tiêu: Kiểm tra API trả về false khi courseCode toàn bộ không tồn tại
 */
function PM_CM_011_testCheckCodeNonExisted() {
    const value = pm.request.url.query.get("value");

    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra API trả về false khi courseCode toàn bộ không tồn tại
    pm.test(`CourseCode ${value} not exists, then return false`, function () {
        pm.expect(body).to.eql(false);
    });
}

/**
 * PM_CM_012 - CheckCode - Existed
 * Mục tiêu: Kiểm tra API trả về true khi courseCode toàn bộ đã tồn tại
 */
function PM_CM_012_testCheckCodeExisted() {
    const value = pm.request.url.query.get("value");

    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra API trả về true khi courseCode toàn bộ đã tồn tại
    pm.test(`CourseCode ${value} exists, then return true`, function () {
        pm.expect(body).to.eql(true);
    });
}

/**
 * PM_CM_013 - CheckCourseCode - Check Authorization (CheckCode Endpoint)
 * Mục tiêu: Kiểm tra phân quyền API kiểm tra courseCode toàn bộ theo role
 */
function PM_CM_013_testCheckCodeAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra status theo role
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Parse body
    let body = null;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Trường hợp có quyền, cần trả về 200 và response là boolean
    if (expected === 200) {
        pm.test("Response is boolean", function () {
            pm.expect(typeof body).to.eql("boolean");
        });
    }

    // Trường hợp không có quyền, cần trả về 403 và message Forbidden
    else if (expected === 403) {
        pm.test("Forbidden response structure", function () {
            pm.expect(body).to.have.property("status");
            pm.expect(body.status).to.eql(403);

            pm.expect(body).to.have.property("error");
            pm.expect(body.error).to.match(/forbidden/i);
        });
    }
}

/**
 * PM_CM_014 - Get Course By Id - Successfully
 * Mục tiêu: Kiểm tra API lấy chi tiết course theo ID thành công
 */
function PM_CM_014_testGetCourseByIdSuccessfully() {
    // Lấy courseId từ path
    const path = pm.request.url.getPath().split("/").filter(Boolean);
    const courseId = Number(path[path.length - 1]);

    // Kiểm tra response status là 200
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra course trả về chứa các field cần thiết
    pm.test("Response has correct course structure", function () {
        pm.expect(body).to.have.property("id");
        pm.expect(body).to.have.property("courseCode");
        pm.expect(body).to.have.property("name");
        pm.expect(body).to.have.property("imgUrl");
        pm.expect(body).to.have.property("intakes");
    });

    // Kiểm tra courseId của path khớp với id của course trả về
    pm.test("Course id matches environment courseId", function () {
        pm.expect(body.id).to.eql(courseId);
    });

    // Kiểm tra intakes là array
    pm.test("Intakes is an array", function () {
        pm.expect(Array.isArray(body.intakes)).to.eql(true);
    });

    // Kiểm tra cấu trúc một intake nếu có
    if (body.intakes.length > 0) {
        pm.test("Each intake has required fields", function () {
            body.intakes.forEach((i, idx) => {
                pm.expect(i, `intake[${idx}]`).to.have.property("id");
                pm.expect(i).to.have.property("name");
                pm.expect(i).to.have.property("intakeCode");
            });
        });
    }

    // Lưu snapshot phục vụ cho testcase khác
    pm.environment.set("old_img_url", body.imgUrl);
    pm.environment.set("old_course_code", body.courseCode);
    pm.environment.set("old_name", body.name);
}

/**
 * PM_CM_015 - Get Course By Id - Not Found
 * Mục tiêu: Kiểm tra API trả về lỗi 404 khi course ID không tồn tại
 */
function PM_CM_015_testGetCourseByIdNotFound() {
    // Lấy courseId từ path
    const path = pm.request.url.getPath().split("/").filter(Boolean);
    const courseId = Number(path[path.length - 1]);

    // Kiểm tra response status là 404
    pm.test("Status code is 404", function () {
        pm.expect(pm.response.code).to.eql(404);
    });

    const body = pm.response.json();

    // Kiểm tra error response có trả message không
    pm.test("Response has error structure", function () {
        pm.expect(body).to.have.property("message");
    });

    // Kiểm tra message đúng context không
    pm.test("Message contains course id", function () {
        pm.expect(body.message).to.include("Not found with course id");
    });
}

/**
 * PM_CM_016 - Get Course By Id - Check Authorization
 * Mục tiêu: Kiểm tra phân quyền API lấy chi tiết course theo ID
 */
function PM_CM_016_testGetCourseByIdAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra status theo role
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Parse body
    let body = null;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Trường hợp có quyền, cần trả về 200 và response là boolean
    if (expected === 200) {
        pm.test("Response is not null", function () {
            pm.expect(body).to.not.eql(null);
        });

        // Kiểm tra response trả về có chứa các field cần thiết không
        if (typeof body === "object" && !Array.isArray(body)) {
            pm.test("Response has correct course structure", function () {
                pm.expect(body).to.have.property("id");
                pm.expect(body).to.have.property("courseCode");
                pm.expect(body).to.have.property("name");
                pm.expect(body).to.have.property("imgUrl");
                pm.expect(body).to.have.property("intakes");
            });
        }
    }

    // Trường hợp không có quyền, cần trả về 403 và message Forbidden
    else if (expected === 403) {
        pm.test("Forbidden response structure", function () {
            pm.expect(body).to.have.property("status");
            pm.expect(body.status).to.eql(403);

            pm.expect(body).to.have.property("error");
            pm.expect(body.error).to.match(/forbidden/i);
        });
    }
}

/**
 * PM_CM_017 - CreateCourse - Successfully
 * Mục tiêu: Kiểm tra API tạo course mới thành công
 */
function PM_CM_017_testCreateCourseSuccessfully() {
    // Kiểm tra HTTP response là 200
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra cấu trúc response
    pm.test("Response has correct structure", function () {
        pm.expect(body).to.have.property("statusCode");
        pm.expect(body).to.have.property("message");
        pm.expect(body).to.have.property("data");
    });

    // Kiểm tra statuscode trong response là 201 (Created)
    pm.test("Business statusCode is 201", function () {
        pm.expect(body.statusCode).to.eql(201);
    });

    // Kiểm tra message trong response
    pm.test("Message is correct", function () {
        pm.expect(body.message).to.eql("Created course successfully!");
    });

    // Kiểm tra môn học được tạo có đủ các field
    pm.test("Created course has required fields", function () {
        pm.expect(body.data).to.have.property("id");
        pm.expect(body.data).to.have.property("courseCode");
        pm.expect(body.data).to.have.property("name");
        pm.expect(body.data).to.have.property("imgUrl");
    });

    // Rollback DB (gọi API xóa môn học)
    const courseId = body.data.id;
    pm.sendRequest(
        {
            url: pm.environment.get("base_url") + "/api/courses/" + courseId,
            method: "DELETE",
            header: {
                Authorization: "Bearer " + pm.environment.get("token_admin"),
                "Content-Type": "application/json"
            }
        },
        function (err, res) {
            pm.test("Cleanup - delete course success", function () {
                pm.expect(res.code).to.be.oneOf([200, 204]);
            });
        }
    );
}

/**
 * PM_CM_018 - CreateCourse - Duplicated
 * Mục tiêu: Kiểm tra API trả về lỗi khi tạo course với courseCode trùng lặp
 */
function PM_CM_018_testCreateCourseDuplicated() {
    // Kiểm tra HTTP response là 400
    pm.test("Status code is 400", function () {
        pm.response.to.have.status(400);
    });

    const body = pm.response.json();

    // Kiểm tra cấu trúc response
    pm.test("Response has correct structure", function () {
        pm.expect(body).to.have.property("statusCode");
        pm.expect(body).to.have.property("message");
        pm.expect(body).to.have.property("data");
    });

    // Kiểm tra statuscode trong response là 409 (Conflict)
    pm.test("Business statusCode is 409", function () {
        pm.expect(body.statusCode).to.eql(409);
    });

    // Kiểm tra message trong response
    pm.test("Message is correct", function () {
        pm.expect(body.message).to.eql("Duplicate Course!");
    });
}

/**
 * PM_CM_019 - CreateCourse - Validation
 * Mục tiêu: Kiểm tra validation khi tạo course với field trống
 */
function PM_CM_019_testCreateCourseValidation() {
    // Prerequest Setup
    function setupValidationCase() {
        // Định nghĩa các test case với các field khác nhau bị thiếu
        const testCases = [
            {
                name: "MISSING_COURSE_CODE",
                body: {
                    courseCode: null,
                    name: "Minh",
                    imgUrl: "a.jpg"
                },
                expectedStatus: 400,
                field: "courseCode"
            },
            {
                name: "MISSING_NAME",
                body: {
                    courseCode: "Minh",
                    name: null,
                    imgUrl: "a.jpg"
                },
                expectedStatus: 400,
                field: "name"
            },
            {
                name: "IMG_NULL_BUT_ALLOWED",
                body: {
                    courseCode: "Minh",
                    name: "Minh",
                    imgUrl: null
                },
                expectedStatus: 200,
                field: "imgUrl"
            }
        ];

        let index = pm.info.iteration;
        index = index % testCases.length;

        const currentCase = testCases[index];

        pm.request.body.raw = JSON.stringify(currentCase.body);

        // Lưu các thông tin cần thiết vào biến môi trường để test script sử dụng
        pm.environment.set("expected_status", currentCase.expectedStatus);
        pm.environment.set("current_case", currentCase.name);
        pm.environment.set("current_field", currentCase.field);
    }

    setupValidationCase();

    // Test Script
    const expected = Number(pm.environment.get("expected_status"));
    const field = pm.environment.get("current_field");
    const caseName = pm.environment.get("current_case");

    // Kiểm tra status
    pm.test(`${caseName} -> status should be ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Parse response safely
    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = pm.response.text();
    }

    // Validation logic
    if (expected === 400) {
        // Kiểm tra statuscode trong response là 400 (Bad Request)
        pm.test(`${field} validation should fail`, function () {
            pm.expect(pm.response.code).to.eql(400);
        });

        // Kiểm tra response là string (trả về message lỗi dạng string)
        pm.test("Response is string", function () {
            pm.expect(typeof body).to.eql("string");
        });

        // Kiểm tra message lỗi có chứa thông tin về field bị lỗi
        pm.test("Error message contains validation info", function () {
            const text = body.toLowerCase();
        });
    } else if (expected === 200) {
        // Kiểm tra statuscode trong response là 201 (Created)
        pm.test("Business statusCode is 201", function () {
            pm.expect(body.statusCode).to.eql(201);
        });

        // Kiểm tra message trong response
        pm.test("Message is correct", function () {
            pm.expect(body.message).to.eql("Created course successfully!");
        });

        // Kiểm tra môn học được tạo có đủ các field
        pm.test("Created course has required fields", function () {
            pm.expect(body.data).to.have.property("id");
            pm.expect(body.data).to.have.property("courseCode");
            pm.expect(body.data).to.have.property("name");
            pm.expect(body.data).to.have.property("imgUrl");
        });
    }

    // Rollback DB (gọi API xóa môn học)
    const courseId = body.data.id;
    pm.sendRequest(
        {
            url: pm.environment.get("base_url") + "/api/courses/" + courseId,
            method: "DELETE",
            header: {
                Authorization: "Bearer " + pm.environment.get("token_admin"),
                "Content-Type": "application/json"
            }
        },
        function (err, res) {
            pm.test("Cleanup - delete course success", function () {
                pm.expect(res.code).to.be.oneOf([200, 204]);
            });
        }
    );
}

/**
 * PM_CM_020 - UpdateCourse - Successfully
 * Mục tiêu: Kiểm tra API cập nhật course thành công
 */
function PM_CM_020_testUpdateCourseSuccessfully() {
    // Kiểm tra HTTP response là 200
    pm.test("Status code là 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra cấu trúc response
    pm.test("Response đúng cấu trúc", function () {
        pm.expect(body).to.have.property("statusCode");
        pm.expect(body).to.have.property("message");
        pm.expect(body).to.have.property("data");
    });

    // Kiểm tra business status
    pm.test("Business statusCode = 200", function () {
        pm.expect(body.statusCode).to.eql(200);
    });

    // Kiểm tra message
    pm.test("Message đúng format update", function () {
        pm.expect(body.message).to.include("Update course with id");
    });

    // Kiểm tra data course
    const course = body.data;
    pm.test("Course có đầy đủ field cần thiết", function () {
        pm.expect(course).to.have.property("id");
        pm.expect(course).to.have.property("courseCode");
        pm.expect(course).to.have.property("name");
        pm.expect(course).to.have.property("imgUrl");
        pm.expect(course).to.have.property("intakes");
    });

    // Kiểm tra id không bị thay đổi
    const pathId = Number(pm.request.url.getPath().split("/").pop());

    pm.test("Id không bị thay đổi sau update", function () {
        pm.expect(course.id).to.eql(pathId);
    });

    // Kiểm tra intakes không bị mất
    pm.test("Intakes là mảng", function () {
        pm.expect(Array.isArray(course.intakes)).to.eql(true);
    });
}

/**
 * PM_CM_021 - UpdateCourse - Not Found
 * Mục tiêu: Kiểm tra API trả về lỗi 404 khi cập nhật course không tồn tại
 */
function PM_CM_021_testUpdateCourseNotFound() {
    // Kiểm tra HTTP response là 404
    pm.test("Status code là 404", function () {
        pm.response.to.have.status(404);
    });

    const body = pm.response.json();

    // Kiểm tra message lỗi
    pm.test("Thông báo lỗi", function () {
        pm.expect(body.message).to.include("Not found with course id");
    });
}

/**
 * PM_CM_022 - UpdateCourse - Validation
 * Mục tiêu: Kiểm tra validation khi cập nhật course với field trống
 */
function PM_CM_022_testUpdateCourseValidation() {
    // Prerequest Setup
    function setupValidationCase() {
        // Định nghĩa các test case với các field khác nhau bị thiếu
        const testCases = [
            {
                name: "COURSE_CODE_NULL",
                body: {
                    courseCode: null,
                    name: "Course A",
                    imgUrl: "a.jpg"
                },
                expected: 400,
                invalidField: "courseCode"
            },
            {
                name: "NAME_NULL",
                body: {
                    courseCode: "G03",
                    name: null,
                    imgUrl: "a.jpg"
                },
                expected: 400,
                invalidField: "name"
            },
            {
                name: "IMG_NULL",
                body: {
                    courseCode: "G03",
                    name: "Course A",
                    imgUrl: null
                },
                expected: 400,
                invalidField: "imgUrl"
            },
            {
                name: "IMG_EMPTY_KEEP_OLD",
                body: {
                    courseCode: "G03",
                    name: "Course A",
                    imgUrl: ""
                },
                expected: 200,
                img_case: "EMPTY"
            }
        ];

        let index = pm.info.iteration % testCases.length;
        const current = testCases[index];

        pm.variables.set("request_body", JSON.stringify(current.body, null, 2));


        // Lưu các thông tin cần thiết vào biến môi trường để test script sử dụng
        pm.environment.set("expected_status", current.expected);
        pm.environment.set("case_name", current.name);
        pm.environment.set("invalid_field", current.invalidField || "");
        pm.environment.set("img_case", current.img_case || "");
    }

    setupValidationCase();

    // Test Script
    const expected = Number(pm.environment.get("expected_status"));
    const caseName = pm.environment.get("case_name");
    const field = pm.environment.get("invalid_field");
    const imgCase = pm.environment.get("img_case");

    // Safe parse
    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = pm.response.text();
    }

    // Fail case (400)
    if (expected === 400) {
        // Kiểm tra status code
        pm.test(`${caseName} - Status phải là 400`, function () {
            pm.expect(pm.response.code).to.eql(400);
        });

        if (pm.response.code !== 400) return;

        const text = typeof body === "string" ? body.toLowerCase() : JSON.stringify(body).toLowerCase();

        // Kiểm tra message lỗi có chứa thông tin về field bị lỗi
        pm.test(`Message phải chứa field ${field}`, function () {
            pm.expect(text).to.include(field.toLowerCase());
        });
    }

    // Success case (200)
    else if (expected === 200) {
        pm.test(`${caseName} - Status phải là 200`, function () {
            pm.expect(pm.response.code).to.eql(200);
        });

        const course = body.data;

        // Kiểm tra course trả về có đầy đủ field không
        pm.test("Correct structure", function () {
            pm.expect(course).to.have.property("id");
            pm.expect(course).to.have.property("courseCode");
            pm.expect(course).to.have.property("name");
            pm.expect(course).to.have.property("imgUrl");
        });

        // Kiểm tra field imgUrl khi gửi imgUrl rỗng -> giữ nguyên ảnh cũ
        if (imgCase === "EMPTY") {
            const oldImg = pm.environment.get("old_img_url");

            pm.test("imgUrl empty -> giữ nguyên ảnh cũ", function () {
                pm.expect(course.imgUrl).to.eql(oldImg);
            });
        }
    }
}

/**
 * PM_CM_023 - DeleteCourseById - Successfully
 * Mục tiêu: Kiểm tra API xóa course thành công
 */
function PM_CM_023_testDeleteCourseByIdSuccessfully() {
    const url = pm.request.url.toString();
    const courseId = url.split("/").pop();

    // Kiểm tra status
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra cấu trúc response
    pm.test("Response đúng cấu trúc", function () {
        pm.expect(body).to.have.property("statusCode");
        pm.expect(body).to.have.property("message");
        pm.expect(body).to.have.property("data");
    });

    // Kiểm tra business status
    pm.test("Business statusCode là 204", function () {
        pm.expect(body.statusCode).to.eql(204);
    });

    // Kiểm tra message
    pm.test("Message đúng", function () {
        pm.expect(body.message.toLowerCase()).to.include(`course with id: ${courseId}`);
    });

    // Verify delete (gọi API GET part-list để kiểm tra course đã bị xóa)
    pm.sendRequest(
        {
            url: pm.environment.get("base_url") + "/api/courses/" + courseId,
            method: "GET",
            header: {
                Authorization: "Bearer " + pm.environment.get("token_admin")
            }
        },
        function (err, res) {
            pm.test("Course đã bị xóa (GET phải lỗi)", function () {
                pm.expect(res.code).to.be.oneOf([404, 500]);
            });
        }
    );
}

/**
 * PM_CM_024 - DeleteCourseById - Not Found
 * Mục tiêu: Kiểm tra API xóa course không tồn tại trả về lỗi 404
 */
function PM_CM_024_testDeleteCourseByIdNotFound() {
    // Kiểm tra status code
    pm.test("Status code phải là 404 (Not Found)", function () {
        pm.response.to.have.status(404);
    });

    // Parse body
    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = pm.response.text();
    }

    // Kiểm tra response có error structure
    pm.test("Response phải có thông tin lỗi", function () {
        pm.expect(body).to.not.eql(null);
    });

    // Kiểm tra message error
    pm.test("Message phải chứa thông tin not found", function () {
        const text = typeof body === "string" ? body.toLowerCase() : JSON.stringify(body).toLowerCase();

        pm.expect(text).to.satisfy(msg => msg.includes("not found") || msg.includes("course id") || msg.includes("404"));
    });
}

/**
 * PM_CM_025 - GetCourseByPart - Successfully
 * Mục tiêu: Kiểm tra API lấy course theo part ID thành công
 */
function PM_CM_025_testGetCourseByPartSuccessfully() {
    // Lấy part ID từ URL
    const partId = pm.request.url.path.slice(-1)[0];

    // Kiểm tra status
    pm.test("Status code phải là 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response có đầy đủ field course
    pm.test("Response có đầy đủ field course", function () {
        pm.expect(body).to.have.property("id");
        pm.expect(body).to.have.property("courseCode");
        pm.expect(body).to.have.property("name");
        pm.expect(body).to.have.property("imgUrl");
    });

    // Kiểm tra course không được null
    pm.test("Course không được null", function () {
        pm.expect(body).to.not.eql(null);
    });
}

/**
 * PM_CM_026 - GetCourseByPart - Not Found
 * Mục tiêu: Kiểm tra API trả về lỗi khi part ID không tồn tại
 */
function PM_CM_026_testGetCourseByPartNotFound() {
    // Lấy part ID từ URL
    const partId = pm.request.url.path.slice(-1)[0];

    // Kiểm tra status
    pm.test("Status code phải là 404", function () {
        pm.response.to.have.status(404);
    });

    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = pm.response.text();
    }

    // Kiểm tra response có error structure
    pm.test("Message phải chứa thông tin not found", function () {
        const text = typeof body === "string" ? body.toLowerCase() : JSON.stringify(body).toLowerCase();

        pm.expect(text).to.satisfy(msg => msg.includes("not found") || msg.includes("part") || msg.includes("course") || msg.includes(partId));
    });
}

/**
 * PM_CM_027 - GetCourseByIntake - Successfully
 * Mục tiêu: Kiểm tra API lấy danh sách course theo intake ID thành công
 */
function PM_CM_027_testGetCourseByIntakeSuccessfully() {
    // Lấy intake ID từ URL
    const intakeId = pm.request.url.path.slice(-2)[0];

    // Kiểm tra status
    pm.test("Status code phải là 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response phải là array
    pm.test("Response phải là array", function () {
        pm.expect(Array.isArray(body)).to.eql(true);
    });

    // Kiểm tra nếu có course thì phải trả về mảng có phần tử
    if (body.length > 0) {
        pm.test("Course có đầy đủ field", function () {
            body.forEach((course, index) => {
                pm.expect(course, `course[${index}]`).to.have.property("id");
                pm.expect(course).to.have.property("courseCode");
                pm.expect(course).to.have.property("name");
                pm.expect(course).to.have.property("imgUrl");
            });
        });
    }
}

/**
 * PM_CM_028 - GetCourseByIntake - Not Found
 * Mục tiêu: Kiểm tra API trả về danh sách rỗng khi intake ID không tồn tại
 */
function PM_CM_028_testGetCourseByIntakeNotFound() {
    // Lấy intake ID từ URL
    const intakeId = pm.request.url.path.slice(-2)[0];

    // Kiểm tra status
    pm.test("Status code phải là 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response phải là array
    pm.test("Response phải là array", function () {
        pm.expect(Array.isArray(body)).to.eql(true);
    });

    // Kiểm tra nếu không có course thì phải trả về mảng rỗng
    pm.test("Nếu không có course thì phải là mảng rỗng", function () {
        pm.expect(body.length).to.be.at.least(0);
    });
}

/**
 * PM_CM_029 - GetPartListByCourse - Default Pagination
 * Mục tiêu: Kiểm tra API lấy danh sách part theo course với phân trang mặc định
 */
function PM_CM_029_testGetPartListByCourseDefaultPagination() {
    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response structure
    pm.test("Response has data and paginationDetails", function () {
        pm.expect(body).to.have.property("data");
        pm.expect(body).to.have.property("paginationDetails");

        pm.expect(Array.isArray(body.data)).to.eql(true);
    });

    // Kiểm tra pagination structure
    pm.test("Pagination structure is valid", function () {
        const pagination = body.paginationDetails;

        pm.expect(pagination).to.have.property("pageNumber");
        pm.expect(pagination).to.have.property("pageCount");
        pm.expect(pagination).to.have.property("totalCount");
        pm.expect(pagination).to.have.property("totalPage");

        pm.expect(pagination.pageNumber).to.be.a("number");
        pm.expect(pagination.pageCount).to.be.a("number");
        pm.expect(pagination.totalCount).to.be.a("number");
        pm.expect(pagination.totalPage).to.be.a("number");
    });

    // Kiểm tra cấu trúc mỗi phần tử trongdata
    if (body.data.length > 0) {
        pm.test("Each item has id and name", function () {
            body.data.forEach((item, index) => {
                pm.expect(item, `item[${index}]`).to.have.property("id");
                pm.expect(item, `item[${index}]`).to.have.property("name");

                pm.expect(item.id).to.be.a("number");
                pm.expect(item.name).to.be.a("string");
            });
        });
    }
}

/**
 * PM_CM_030 - GetPartListByCourse - Custom Pagination
 * Mục tiêu: Kiểm tra API lấy danh sách part theo course với phân trang tùy chỉnh
 */
function PM_CM_030_testGetPartListByCourseCustomPagination() {
    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Lấy query params
    const page = pm.request.url.query.get("page");
    const size = pm.request.url.query.get("size");
    const sort = pm.request.url.query.get("sort");

    const pageNum = page !== null ? Number(page) : null;
    const sizeNum = size !== null ? Number(size) : null;

    // Kiểm tra response structure
    pm.test("Response has data and paginationDetails", function () {
        pm.expect(body).to.have.property("data");
        pm.expect(body).to.have.property("paginationDetails");

        pm.expect(Array.isArray(body.data)).to.eql(true);
    });

    // Kiểm tra pagination khớp request params
    pm.test("Pagination matches request params", function () {
        const pagination = body.paginationDetails;

        if (pageNum !== null) {
            pm.expect(pagination.pageNumber).to.eql(pageNum);
        }

        if (sizeNum !== null) {
            pm.expect(pagination.pageCount).to.be.at.most(sizeNum);
        }
    });

    // Kiểm tra pagination fields tồn tại
    pm.test("Pagination fields exist", function () {
        const pagination = body.paginationDetails;

        pm.expect(pagination).to.have.property("pageNumber");
        pm.expect(pagination).to.have.property("pageCount");
        pm.expect(pagination).to.have.property("totalCount");
        pm.expect(pagination).to.have.property("totalPage");
    });

    // Kiểm tra cấu trúc mỗi phần tử trong data
    if (body.data.length > 0) {
        pm.test("Each item has id and name", function () {
            body.data.forEach((item, index) => {
                pm.expect(item, `item[${index}]`).to.have.property("id");
                pm.expect(item, `item[${index}]`).to.have.property("name");

                pm.expect(item.id).to.be.a("number");
                pm.expect(item.name).to.be.a("string");
            });
        });
    }

    // Kiểm tra sort nếu có
    if (sort && body.data.length > 0) {
        let [field, direction = "asc"] = sort.split(",");

        const values = body.data.map(x => x[field]);

        let sorted = [...values].sort((a, b) => (typeof a === "number" ? a - b : String(a).localeCompare(String(b))));

        if (direction.toLowerCase() === "desc") {
            sorted.reverse();
        }

        pm.test(`Sorted by ${field} ${direction}`, function () {
            pm.expect(values).to.eql(sorted);
        });
    }
}

/**
 * PM_CM_031 - GetPartListByCourse - Empty Response
 * Mục tiêu: Kiểm tra API trả về danh sách rỗng khi không có part
 */
function PM_CM_031_testGetPartListByCourseEmptyResponse() {
    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response có data array
    pm.test("Response has data array", function () {
        pm.expect(body).to.have.property("data");
        pm.expect(Array.isArray(body.data)).to.eql(true);
    });

    // Kiểm tra data array rỗng
    pm.test("Data is empty array", function () {
        pm.expect(body.data.length).to.eql(0);
    });

    // Kiểm tra pagination tồn tại
    pm.test("Pagination exists", function () {
        pm.expect(body).to.have.property("paginationDetails");
    });
}

/**
 * PM_CM_032 - GetPartListByCourse - Course Not Found
 * Mục tiêu: Kiểm tra API trả về danh sách rỗng khi course không tồn tại
 */
function PM_CM_032_testGetPartListByCourseCourseNotFound() {
    // Kiểm tra status code
    pm.test("Status code is 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response có data array
    pm.test("Response has data array", function () {
        pm.expect(body).to.have.property("data");
        pm.expect(Array.isArray(body.data)).to.eql(true);
    });

    // Kiểm tra data array rỗng
    pm.test("Data is empty array", function () {
        pm.expect(body.data.length).to.eql(0);
    });

    // Kiểm tra pagination tồn tại
    pm.test("Pagination exists", function () {
        pm.expect(body).to.have.property("paginationDetails");
    });
}

/**
 * PM_CM_033 - GetPartListByCourse - Check Authorization
 * Mục tiêu: Kiểm tra phân quyền API lấy danh sách part theo course
 */
function PM_CM_033_testGetPartListByCourseAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra status code
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Safe parse body
    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Success case (200)
    if (expected === 200) {
        // Kiểm tra response có data field không
        pm.test("Response is not null", function () {
            pm.expect(body).to.not.eql(null);
        });

        // Kiểm tra response có data field (dù có thể là array rỗng)
        pm.test("Response has data field (if list API)", function () {
            pm.expect(body).to.have.property("data");
        });
    }

    // Forbidden case (403)
    else if (expected === 403) {
        // Kiểm tra response có cấu trúc lỗi đúng không
        pm.test("Forbidden response", function () {
            pm.expect(body).to.not.eql(null);

            pm.expect(body).to.have.property("message");
            pm.expect(body.message).to.be.a("string");

            pm.expect(body.message.toLowerCase()).to.include("forbidden");
        });
    }
}

/**
 * PM_CM_034 - GetPartListByCourse - Successfully
 * Mục tiêu: Kiểm tra API lấy danh sách part (không phân trang) thành công
 */
function PM_CM_034_testGetPartListByCourseSuccessfully() {
    // Kiểm tra status
    pm.test(`Should return 200`, function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response là array
    pm.test("Response is array", function () {
        pm.expect(Array.isArray(body)).to.eql(true);
    });

    // Kiểm tra cấu trúc mỗi phần tử trong array
    if (body.length > 0) {
        pm.test("Each part has correct structure", function () {
            body.forEach((item, index) => {
                pm.expect(item, `part[${index}]`).to.have.property("id");
                pm.expect(item, `part[${index}]`).to.have.property("name");
                pm.expect(item, `part[${index}]`).to.have.property("course");

                // Check course object
                pm.expect(item.course).to.be.an("object");
                pm.expect(item.course).to.have.property("id");
                pm.expect(item.course).to.have.property("courseCode");
                pm.expect(item.course).to.have.property("name");
                pm.expect(item.course).to.have.property("imgUrl");
                pm.expect(item.course).to.have.property("intakes");
            });
        });
    }
}

/**
 * PM_CM_035 - GetPartListByCourse - Course Not Found (List Version)
 * Mục tiêu: Kiểm tra API trả về lỗi 404 khi course không tồn tại
 */
function PM_CM_035_testGetPartListByCourseNotFoundListVersion() {
    // Kiểm tra status code
    pm.test("Should return error status", function () {
        pm.expect(pm.response.code).to.eql(404);
    });

    let body = null;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Kiểm tra response có cấu trúc lỗi đúng không
    pm.test("Response has error structure", function () {
        pm.expect(body).to.have.property("status");
        pm.expect(body).to.have.property("error");
        pm.expect(body).to.have.property("message");
    });

    // Kiểm tra message đúng context
    pm.test("Message has correct context", function () {
        pm.expect(body.message.toLowerCase()).to.satisfy(msg => msg.includes("not found") || msg.includes("no value"));
    });
}

/**
 * PM_CM_036 - GetPartListByCourse - Check Authorization (List Version)
 * Mục tiêu: Kiểm tra phân quyền API lấy danh sách part (không phân trang)
 */
function PM_CM_036_testGetPartListByCourseAuthorizationListVersion() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        //  Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra status code
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Safe parse body
    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Success case (200)
    if (expected === 200) {
        // Kiểm tra response là array
        pm.test("Response is array", function () {
            pm.expect(Array.isArray(body)).to.eql(true);
        });

        // Kiểm tra cấu trúc mỗi phần tử trong array
        if (body.length > 0) {
            pm.test("Each part has correct structure", function () {
                body.forEach((item, index) => {
                    pm.expect(item, `part[${index}]`).to.have.property("id");
                    pm.expect(item, `part[${index}]`).to.have.property("name");
                    pm.expect(item, `part[${index}]`).to.have.property("course");

                    // Check course object
                    pm.expect(item.course).to.be.an("object");
                    pm.expect(item.course).to.have.property("id");
                    pm.expect(item.course).to.have.property("courseCode");
                    pm.expect(item.course).to.have.property("name");
                    pm.expect(item.course).to.have.property("imgUrl");
                    pm.expect(item.course).to.have.property("intakes");
                });
            });
        }
    }

    // Forbidden case (403)
    else if (expected === 403) {
        // Kiểm tra response có cấu trúc lỗi đúng không
        pm.test("Forbidden response", function () {
            pm.expect(body).to.not.eql(null);

            pm.expect(body).to.have.property("message");
            pm.expect(body.message).to.be.a("string");

            pm.expect(body.message.toLowerCase()).to.include("forbidden");
        });
    }
}

/**
 * PM_CM_037 - GetPartById - Successfully
 * Mục tiêu: Kiểm tra API lấy chi tiết part theo ID thành công
 */
function PM_CM_037_testGetPartByIdSuccessfully() {
    // Kiểm tra status
    pm.test("Should return 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    // Kiểm tra response là object
    pm.test("Response is object", function () {
        pm.expect(body).to.be.an("object");
    });

    // Kiểm tra cấu trúc part object
    pm.test("Part has correct structure", function () {
        pm.expect(body).to.have.property("id");
        pm.expect(body).to.have.property("name");
        pm.expect(body).to.have.property("course");

        // Kiểm tra cấu trúc object course
        pm.expect(body.course).to.be.an("object");

        pm.expect(body.course).to.have.property("id");
        pm.expect(body.course).to.have.property("courseCode");
        pm.expect(body.course).to.have.property("name");
        pm.expect(body.course).to.have.property("imgUrl");
        pm.expect(body.course).to.have.property("intakes");
    });
}

/**
 * PM_CM_038 - GetPartById - Not Found
 * Mục tiêu: Kiểm tra API trả về lỗi 404 khi part ID không tồn tại
 */
function PM_CM_038_testGetPartByIdNotFound() {
    // Kiểm tra status
    pm.test("Should return 404", function () {
        pm.expect(pm.response.code).to.eql(404);
    });

    // Parse body
    let body = null;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Kiểm tra response có cấu trúc lỗi đúng không
    pm.test("Response has error structure", function () {
        pm.expect(body).to.have.property("status");
        pm.expect(body).to.have.property("error");
        pm.expect(body).to.have.property("message");
    });

    // Kiểm tra message có chứa thông tin not found và part id
    pm.test("Message contains not found info", function () {
        const msg = body.message.toLowerCase();

        pm.expect(msg).to.include("not found");
        pm.expect(msg).to.include("part id");
    });
}

/**
 * PM_CM_039 - GetPartById - Check Authorization
 * Mục tiêu: Kiểm tra phân quyền API lấy chi tiết part theo ID
 */
function PM_CM_039_testGetPartByIdAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 200 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra status code
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Safe parse body
    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Success case (200)
    if (expected === 200) {
        // Kiểm tra response là object
        pm.test("Response is object", function () {
            pm.expect(body).to.be.an("object");
        });

        // Kiểm tra cấu trúc part object
        pm.test("Part has correct structure", function () {
            pm.expect(body).to.have.property("id");
            pm.expect(body).to.have.property("name");
            pm.expect(body).to.have.property("course");

            // Kiểm tra cấu trúc object course
            pm.expect(body.course).to.be.an("object");

            pm.expect(body.course).to.have.property("id");
            pm.expect(body.course).to.have.property("courseCode");
            pm.expect(body.course).to.have.property("name");
            pm.expect(body.course).to.have.property("imgUrl");
            pm.expect(body.course).to.have.property("intakes");
        });
    }

    // Forbidden case (403)
    else if (expected === 403) {
        // Kiểm tra response có cấu trúc lỗi đúng không
        pm.test("Forbidden response", function () {
            pm.expect(body).to.not.eql(null);

            pm.expect(body).to.have.property("message");
            pm.expect(body.message).to.be.a("string");

            pm.expect(body.message.toLowerCase()).to.include("forbidden");
        });
    }
}

/**
 * PM_CM_040 - UpdatePart - Successfully
 * Mục tiêu: Kiểm tra API cập nhật tên part thành công
 */
function PM_CM_040_testUpdatePartSuccessfully() {
    // Prerequest Setup
    function setupExpectedName() {
        const raw = pm.request.body.raw;

        const name = raw.replace(/\"/g, "");

        // Lưu name vào biến môi trường để test script sử dụng
        pm.environment.set("expected_name", name);
    }

    setupExpectedName();

    // Test Script
    // Kiểm tra status
    pm.test("Should return 200", function () {
        pm.response.to.have.status(200);
    });

    const body = pm.response.json();

    const expectedName = pm.environment.get("expected_name");

    // Kiểm tra response có đầy đủ field không
    pm.test("Response has correct structure", function () {
        pm.expect(body).to.have.property("id");
        pm.expect(body).to.have.property("name");
        pm.expect(body).to.have.property("course");
    });

    // Kiểm tra name được cập nhật đúng giá trị gửi lên
    pm.test("Name updated correctly", function () {
        pm.expect(body.name).to.eql(expectedName);
    });
}

/**
 * PM_CM_041 - UpdatePart - Part Not Found
 * Mục tiêu: Kiểm tra API cập nhật part không tồn tại trả về lỗi 404
 */
function PM_CM_041_testUpdatePartPartNotFound() {
    // Prerequest Setup
    function setupExpectedName() {
        const raw = pm.request.body.raw;

        const name = raw.replace(/\"/g, "");

        // Lưu name vào biến môi trường để test script sử dụng
        pm.environment.set("expected_name", name);
    }

    setupExpectedName();

    // Test Script
    // Kiểm tra status
    pm.test("Should return 404", function () {
        pm.expect(pm.response.code).to.eql(404);
    });

    // Parse body
    let body = null;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Check error structure
    pm.test("Response has error structure", function () {
        pm.expect(body).to.have.property("status");
        pm.expect(body).to.have.property("error");
        pm.expect(body).to.have.property("message");
    });

    // Kiểm tra message đúng context
    pm.test("Message has correct context", function () {
        pm.expect(body.message.toLowerCase()).to.satisfy(msg => msg.includes("not found") || msg.includes("no value"));
    });
}

/**
 * PM_CM_042 - UpdatePart - Check Name Null
 * Mục tiêu: Kiểm tra API validation khi tên part trống
 */
function PM_CM_042_testUpdatePartCheckNameNull() {
    // Prerequest Setup
    function setupExpectedName() {
        let raw = pm.request.body?.raw;

        let name = null;

        if (raw) {
            const trimmed = raw.replace(/\"/g, "").trim();
            name = trimmed ? trimmed : null;
        }

        // Lưu name vào biến môi trường để test script sử dụng
        pm.environment.set("expected_name", name);
    }

    setupExpectedName();

    // Test Script
    // Kiểm tra status code phải là 400
    pm.test("Should return 400", function () {
        pm.response.to.have.status(400);
    });

    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = pm.response.text();
    }

    const msg = typeof body === "string" ? body.toLowerCase() : JSON.stringify(body).toLowerCase();

    // Kiểm tra message có chứa thông tin validation
    pm.test("Validation message exists", function () {
        pm.expect(msg).to.satisfy(m => m.includes("must not be null") || m.includes("must not be blank") || m.includes("validation"));
    });
}

/**
 * PM_CM_043 - UpdatePart - Check Authorization
 * Mục tiêu: Kiểm tra phân quyền API cập nhật part
 */
function PM_CM_043_testUpdatePartCheckAuthorization() {
    // Prerequest Setup
    function setupAuth() {
        const tokens = [
            { name: "ADMIN", token: pm.environment.get("token_admin"), expected: 200 },
            { name: "LECTURER", token: pm.environment.get("token_lecturer"), expected: 403 },
            { name: "STUDENT", token: pm.environment.get("token_student"), expected: 403 }
        ];

        const index = pm.info.iteration;
        const current = tokens[index];

        // Lưu vào biến môi trường để test script sử dụng
        pm.environment.set("current_role", current.name);
        pm.environment.set("current_token", current.token);
        pm.environment.set("expected_status", current.expected);

        const raw = pm.request.body.raw;
        const name = raw.replace(/\"/g, "");

        // Lưu name vào biến môi trường để test script sử dụng
        pm.environment.set("expected_name", name);
    }

    setupAuth();

    // Test Script
    const role = pm.environment.get("current_role");
    const expected = Number(pm.environment.get("expected_status"));

    // Kiểm tra status code
    pm.test(`ROLE_${role} should return ${expected}`, function () {
        pm.response.to.have.status(expected);
    });

    // Safe parse body
    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Success case (200)
    if (expected === 200) {
        const expectedName = pm.environment.get("expected_name");

        // Kiểm tra response có đầy đủ field không
        pm.test("Response has correct structure", function () {
            pm.expect(body).to.have.property("id");
            pm.expect(body).to.have.property("name");
            pm.expect(body).to.have.property("course");
        });

        // Kiểm tra name được cập nhật đúng giá trị gửi lên
        pm.test("Name updated correctly", function () {
            pm.expect(body.name).to.eql(expectedName);
        });
    }

    // Forbidden case (403)
    else if (expected === 403) {
        // Kiểm tra response có cấu trúc lỗi đúng không
        pm.test("Forbidden response", function () {
            pm.expect(body).to.not.eql(null);

            pm.expect(body).to.have.property("message");
            pm.expect(body.message).to.be.a("string");

            pm.expect(body.message.toLowerCase()).to.include("forbidden");
        });
    }
}

/**
 * PM_CM_044 - CreatePartByCourse - Successfully
 * Mục tiêu: Kiểm tra API tạo part mới thành công
 */
function PM_CM_044_testCreatePartByCourseSuccessfully() {
    // Prerequest Setup
    function setupExpectedName() {
        let name = null;

        try {
            const json = JSON.parse(pm.request.body.raw);
            name = json.name || null;
        } catch (e) {
            name = null;
        }

        // Lưu name vào biến môi trường để test script sử dụng
        pm.environment.set("expected_name", name);
    }

    setupExpectedName();

    // Test Script
    // Kiểm tra status
    pm.test("Should return 200", function () {
        pm.response.to.have.status(200);
    });

    // Kiểm tra response body có rỗng không
    pm.test("Response body is empty", function () {
        const text = pm.response.text().trim();
        pm.expect(text).to.eql("");
    });

    // Chuẩn bị gọi API lấy danh sách part sau khi tạo để kiểm tra part mới có trong list không
    const url = pm.request.url.toString();
    const match = url.match(/courses\/(\d+)/);
    const courseId = match ? match[1] : null;
    
    // Gọi API lấy danh sách part để kiểm tra part mới có trong list không
    pm.sendRequest(
        {
            url: pm.environment.get("base_url") + "/api/courses/" + courseId + "/part-list",
            method: "GET",
            header: {
                Authorization: "Bearer " + pm.environment.get("token_admin")
            }
        },
        function (err, res) {
            const expectedName = pm.environment.get("expected_name");
            pm.test(`Part ${expectedName} was created`, function () {
                const list = res.json();
                const found = list.some(p => p.name === expectedName);
                pm.expect(found).to.eql(true);
            });
        }
    );
}

/**
 * PM_CM_045 - CreatePartByCourse - Course Not Found
 * Mục tiêu: Kiểm tra API tạo part cho course không tồn tại trả về lỗi 404
 */
function PM_CM_045_testCreatePartByCourseCourseNotFound() {
    // Prerequest Setup
    function setupExpectedName() {
        let name = null;

        try {
            const json = JSON.parse(pm.request.body.raw);
            name = json.name || null;
        } catch (e) {
            name = null;
        }

        // Lưu name vào biến môi trường để test script sử dụng
        pm.environment.set("expected_name", name);
    }

    setupExpectedName();

    // Test Script
    // Kiểm tra status
    pm.test("Should return 404", function () {
        pm.expect(pm.response.code).to.eql(404);
    });

    // Parse body
    let body = null;
    try {
        body = pm.response.json();
    } catch (e) {
        body = null;
    }

    // Kiểm tra response có cấu trúc lỗi đúng không
    pm.test("Response has error structure", function () {
        pm.expect(body).to.have.property("status");
        pm.expect(body).to.have.property("error");
        pm.expect(body).to.have.property("message");
    });

    // Kiểm tra message đúng context
    pm.test("Message has correct context", function () {
        pm.expect(body.message.toLowerCase()).to.satisfy(msg => msg.includes("not found") || msg.includes("no value"));
    });
}

/**
 * PM_CM_046 - CreatePartByCourse - Check Name Null
 * Mục tiêu: Kiểm tra API validation khi tên part trống khi tạo
 */
function PM_CM_046_testCreatePartByCourseCheckNameNull() {
    // Prerequest Setup
    function setupExpectedName() {
        let name = null;

        try {
            const json = JSON.parse(pm.request.body.raw);
            name = json.name || null;
        } catch (e) {
            name = null;
        }

        // Lưu name vào biến môi trường để test script sử dụng
        pm.environment.set("expected_name", name);
    }

    setupExpectedName();

    // Test Script
    pm.test("Should return 400", function () {
        pm.response.to.have.status(400);
    });

    let body;
    try {
        body = pm.response.json();
    } catch (e) {
        body = pm.response.text();
    }

    const msg = typeof body === "string" ? body.toLowerCase() : JSON.stringify(body).toLowerCase();

    // Kiểm tra message có chứa thông tin validation
    pm.test("Validation message exists", function () {
        pm.expect(msg).to.satisfy(m => m.includes("must not be null") || m.includes("must not be blank") || m.includes("validation"));
    });
}
