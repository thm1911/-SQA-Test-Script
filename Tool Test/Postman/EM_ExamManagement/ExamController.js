//PM_EM_001
//Kiểm tra lấy danh sách exam theo trang thành công
pm.test("Status 200", () => pm.response.to.have.status(200));

const contentType = pm.response.headers.get("Content-Type") || "";
pm.test("Content-Type là JSON", () => {
  pm.expect(contentType).to.include("application/json");
});

const body = pm.response.json();
pm.test("PageResult hợp lệ", () => {
  pm.expect(body).to.be.an("object");
  pm.expect(body).to.have.property("data");
  pm.expect(body.data).to.be.an("array");
});

//PM_EM_002
//Kiểm tra lấy danh sách exam theo trang (student token)
pm.test("STUDENT không được truy cập danh sách exam quản trị", () => {
  pm.expect(pm.response.code).to.eql(403);
});

//PM_EM_003
//Kiểm tra lấy danh sách exam theo trang (student token)
pm.test("Phải đăng nhập mới có thể call endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});

//PM_EM_004
//Kiểm tra lấy danh sách bài kiểm tra của user (đã đăng nhập) thành công
pm.test("Status 200", () => pm.response.to.have.status(200));
const body = pm.response.json();

pm.test("Trả về danh sách examUser", () => {
  pm.expect(body).to.be.an("array");
});

pm.test("Mỗi item có exam.locked kiểu boolean", () => {
  body.forEach(item => {
    pm.expect(item).to.have.property("exam");
    pm.expect(item.exam).to.have.property("locked");
    pm.expect(item.exam.locked).to.be.a("boolean");
  });
});

//PM_EM_005
//Kiểm tra lấy danh sách bài kiểm tra của user (chưa đăng nhập) - kiểm tra xác thực

pm.test("Phải đăng nhập mới có thể call endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});

//PM_EM_006
//Kiểm tra lấy exam-user theo examId thành công
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(200);
});

if (pm.response.code === 200) {
  const body = pm.response.json();
  pm.test("Có dữ liệu examUser", () => {
    pm.expect(body).to.be.an("object");
    pm.expect(body).to.have.property("exam");
    pm.expect(body.exam).to.be.an("object");
  });
}

//PM_EM_007
//Kiểm tra lấy exam-user theo examId trường hợp exam không tồn tại
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(404);
});

//PM_EM_008
//Kiểm tra lấy exam-user theo examId trường hợp chưa đăng nhập
pm.test("Phải đăng nhập mới có thể call endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});

//PM_EM_009
//Kiểm tra lấy danh sách câu hỏi thành công
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(200);
});

if (pm.response.code === 200) {
  const body = pm.response.json();
  pm.test("Cấu trúc ExamQuestionList hợp lệ", () => {
    pm.expect(body).to.be.an("object");
    pm.expect(body).to.have.property("remainingTime");
    pm.expect(body.remainingTime).to.be.a("number");
    pm.expect(body).to.have.property("questions");
    pm.expect(body.questions).to.be.an("array");
  });
}

//PM_EM_010
//Kiểm tra lấy danh sách câu hỏi trường hợp exam bị khóa hoặc chưa đến giờ làm bài
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(400);
});

//PM_EM_011
//Kiểm tra lấy danh sách câu hỏi trường hợp chưa đăng nhập
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(404);
});

//PM_EM_012
//Kiểm tra lấy danh sách câu hỏi trường hợp exam không tồn tại
pm.test("Phải đăng nhập mới có thể call endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});

//PM_EM_013
//Kiểm tra tạo exam thành công
pm.test("Create exam status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(200);
});

if (pm.response.code === 200) {
  const body = pm.response.json();
  pm.test("Response có exam vừa tạo", () => {
    pm.expect(body).to.be.an("object");
    pm.expect(body).to.have.property("id");
    pm.expect(body).to.have.property("questionData");
  });
  pm.environment.set("examId", String(body.id));
}

//PM_EM_014
//Kiểm tra tạo exam với title rỗng
pm.test("Create exam thất bại", () => {
  pm.expect(pm.response.code).to.eql(400);
});


if (pm.response.code === 400) {
  const body = pm.response.json();
  pm.test("Response có message lỗi", () => {
    pm.expect(body).to.have.property("message");
  });
}

//PM_EM_015
//Kiểm tra tạo exam với durationExam rỗng
pm.test("Create exam thất bại", () => {
  pm.expect(pm.response.code).to.eql(400);
});


if (pm.response.code === 400) {
  const body = pm.response.json();
  pm.test("Response có message lỗi", () => {
    pm.expect(body).to.have.property("message");
  });
}

//PM_EM_016
//Kiểm tra tạo exam với beginExam rỗng
pm.test("Create exam thất bại", () => {
  pm.expect(pm.response.code).to.eql(400);
});


if (pm.response.code === 400) {
  const body = pm.response.json();
  pm.test("Response có message lỗi", () => {
    pm.expect(body).to.have.property("message");
  });
}

//PM_EM_017
//Kiểm tra tạo exam với finishExam rỗng
pm.test("Create exam thất bại", () => {
  pm.expect(pm.response.code).to.eql(400);
});


if (pm.response.code === 400) {
  const body = pm.response.json();
  pm.test("Response có message lỗi", () => {
    pm.expect(body).to.have.property("message");
  });
}

//PM_EM_018
//Kiểm tra tạo exam với questionData rỗng
pm.test("Create exam thất bại", () => {
  pm.expect(pm.response.code).to.eql(400);
});


if (pm.response.code === 400) {
  const body = pm.response.json();
  pm.test("Response có message lỗi", () => {
    pm.expect(body).to.have.property("message");
  });
}

//PM_EM_019
//Kiểm tra tạo exam với intake không tồn tại
pm.test("Create exam thất bại", () => {
  pm.expect(pm.response.code).to.eql(400);
});


if (pm.response.code === 400) {
  const body = pm.response.json();
  pm.test("Response có message lỗi", () => {
    pm.expect(body).to.have.property("message");
  });
}

//PM_EM_020
//Kiểm tra tạo exam với part không tồn tại
pm.test("Create exam thất bại", () => {
  pm.expect(pm.response.code).to.eql(400);
});


if (pm.response.code === 400) {
  const body = pm.response.json();
  pm.test("Response có message lỗi", () => {
    pm.expect(body).to.have.property("message");
  });
}

//PM_EM_021
//Kiểm tra tạo exam với tài khoản student (kiểm tra phân quyền)

pm.test("STUDENT không được call endpoint này", () => {
  pm.expect(pm.response.code).to.eql(403);
});

//PM_EM_022
//Kiểm tra tạo exam chưa đăng nhập tài khoản(kiểm tra xác thực)

pm.test("Phải đăng nhập mới có thể call endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});

//PM_EM_023
//Kiểm tra lấy chi tiết exam theo id thành công
pm.test("Status 200", () => pm.response.to.have.status(200));
const body = pm.response.json();

pm.test("Exam id đúng", () => {
  pm.expect(body).to.be.an("object");
  pm.expect(body.id).to.eql(216);
});

//PM_EM_024
//Kiểm tra lấy chi tiết exam theo id với examId không tồn tại
pm.test("Status 404", () => pm.response.to.have.status(404));

//PM_EM_025
//Kiểm tra lấy chi tiết exam theo id khi chưa đăng nhập
pm.test("Status 401", () => pm.response.to.have.status(401));

//PM_EM_026
//Kiểm tra lưu bài làm tạm thời thành công
pm.test("Lưu bài làm status hợp lệ", () => {
  pm.expect(pm.response.code).to.be.oneOf([200, 204]);
});


//PM_EM_027
//Kiểm tra lưu đáp án với exam id không tồn tại
pm.test("Trả về status 400", () => {
  pm.expect(pm.response.code).to.eql(400);
});

//PM_EM_028
//Kiểm tra lưu đáp án với remaingTime âm
pm.test("Trả về status 400", () => {
  pm.expect(pm.response.code).to.eql(400);
});

//PM_EM_029
//Kiểm tra lưu đáp án khi chưa đăng nhập
pm.test("Phải đăng nhập mới có thể call endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});


//PM_EM_030
//Kiểm tra lấy kết quả toàn bộ user của exam thành công (admin hoặc lecturer)
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(200);
});

if (pm.response.code === 200) {
  const body = pm.response.json();
  pm.test("Danh sách kết quả hợp lệ", () => {
    pm.expect(body).to.be.an("array");
    body.forEach(item => {
      pm.expect(item).to.have.property("examStatus");
      pm.expect([-2, -1, 0, 1]).to.include(item.examStatus);
    });
  });
}


//PM_EM_031
//Kiểm tra lấy kết quả toàn bộ user của exam của 1 exam không tồn tại
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(404);
});


//PM_EM_032
//Kiểm tra lấy kết quả toàn bộ user của exam của 1 exam với
//tài khoản student (kiểm tra phân quyền)
pm.test("Student không có quyền gọi endpoint này", () => {
  pm.expect(pm.response.code).to.eql(403);
});

//PM_EM_033
//Kiểm tra lấy kết quả toàn bộ user của exam của 1 exam khi
//chưa đăng nhập (kiểm tra xác thực)
pm.test("Phải đăng nhập để truy cập endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});

//PM_EM_034
//Kiểm tra thống kê câu hỏi trong bài thi thành công
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(200);
});

const contentType = pm.response.headers.get("Content-Type") || "";
if (pm.response.code === 200 && contentType.includes("application/json")) {
  const body = pm.response.json();
  if (Array.isArray(body)) {
    pm.test("Question report hợp lệ", () => {
      body.forEach(r => {
        pm.expect(r).to.have.property("correctTotal");
        pm.expect(r.correctTotal).to.be.at.least(0);
      });
    });
  }
}

//PM_EM_035
//Kiểm tra thống kê câu hỏi trong bài thi với exam không tồn tại
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(404);
});

//PM_EM_036
//Kiểm tra thống kê câu hỏi trong bài thi với tài khoản student
//(kiểm tra phân quyền)
pm.test("Student không có quyền truy cập endpoint này", () => {
  pm.expect(pm.response.code).to.eql(403);
});


//PM_EM_037
//Kiểm tra thống kê câu hỏi trong bài thi khi chưa đăng nhập (kiểm tra xác thực)
pm.test("Phải đăng nhập để truy cập endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});


//PM_EM_038
//Kiểm tra lấy kết quả bài thi của user hiện tại
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(200);
});

if (pm.response.code === 200) {
  const body = pm.response.json();
  pm.test("Exam result có choiceList và totalPoint", () => {
    pm.expect(body).to.have.property("choiceList");
    pm.expect(body).to.have.property("totalPoint");
    pm.expect(body.choiceList).to.be.an("array");
  });

  pm.test("totalPoint khớp choiceList", () => {
    const expected = (body.choiceList || []).reduce((sum, c) => {
      return sum + (c.isSelectedCorrected ? Number(c.point || 0) : 0);
    }, 0);
    pm.expect(Number(body.totalPoint)).to.eql(expected);
  });
}

//PM_EM_039
//Kiểm tra lấy kết quả bài thi của user hiện tại với exam không tồn tại
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(404);
});

//PM_EM_040
//Kiểm tra lấy kết quả bài thi của user hiện tại khi chưa đăng nhập
pm.test("Phải đăng nhập để truy cập endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});

//PM_EM_041
//Kiểm tra lấy kết quả bài thi theo username thành công
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(200);
});

if (pm.response.code === 200) {
  const body = pm.response.json();
  pm.test("Result theo user hợp lệ", () => {
    pm.expect(body).to.have.property("user");
    pm.expect(body).to.have.property("userTimeBegin");
    pm.expect(body).to.have.property("userTimeFinish");
    pm.expect(body).to.have.property("remainingTime");
    pm.expect(body).to.have.property("choiceList");
  });

  pm.test("totalPoint khớp choiceList", () => {
    const expected = (body.choiceList || []).reduce((sum, c) => {
      return sum + (c.isSelectedCorrected ? Number(c.point || 0) : 0);
    }, 0);
    pm.expect(Number(body.totalPoint)).to.eql(expected);
  });
}

//PM_EM_042
//Kiểm tra lấy kết quả bài thi theo username với examId không tồn tại
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(404);
});

//PM_EM_043
//Kiểm tra lấy kết quả bài thi theo username với username không tồn tại
pm.test("Status hợp lệ", () => {
  pm.expect(pm.response.code).to.eql(404);
});

//PM_EM_044
//Kiểm tra lấy kết quả bài thi theo username khi chưa đăng nhập (kiểm tra xác thực)
pm.test("Phải đăng nhập để truy cập endpoint này", () => {
  pm.expect(pm.response.code).to.eql(401);
});


//PM_EM_045
//Kiểm tra lấy danh sách câu hỏi dạng text theo exam thành công
pm.test("Status 200", () => pm.response.to.have.status(200));
const body = pm.response.json();

pm.test("Danh sách câu hỏi của exam hợp lệ", () => {
  pm.expect(body).to.be.an("array");
  body.forEach(item => {
    pm.expect(item).to.have.property("questionText");
    pm.expect(item).to.have.property("point");
    pm.expect(item).to.have.property("difficultyLevel");
    pm.expect(item).to.have.property("questionType");
  });
});

//PM_EM_046
//Kiểm tra lấy danh sách câu hỏi dạng text theo exam với exam id không tồn tại
pm.test("Exam không tồn tại", () => pm.response.to.have.status(404));

//PM_EM_047
//Kiểm tra lấy danh sách câu hỏi dạng text theo exam
//khi chưa đăng nhập (kiểm tra xác thực)
pm.test("Phải đăng nhập để truy cập endpoint này", () => pm.response.to.have.status(404));

//PM_EM_048
//Kiểm tra lấy lịch thi của user hiện tại thành công
pm.test("Status 200", () => pm.response.to.have.status(200));
const body = pm.response.json();

pm.test("Schedule hợp lệ", () => {
  pm.expect(body).to.be.an("array");
  body.forEach(item => {
    pm.expect(item).to.have.property("completeString");
    pm.expect(item).to.have.property("isCompleted");
    pm.expect(["Missed", "Not yet started", "Completed", "Doing"]).to.include(item.completeString);
    pm.expect([-2, -1, 0, 1]).to.include(item.isCompleted);
  });
});

//PM_EM_049
//Kiểm tra lấy lịch thi của user hiện tại khi chưa đăng nhập (kiểm tra xác thực)
pm.test("Phải đăng nhập để truy cập endpoint này", () => pm.response.to.have.status(401));

//PM_EM_050
//Kiểm tra hủy exam khi chưa bắt đầu
// Endpoint này dùng sai method, không viết script test