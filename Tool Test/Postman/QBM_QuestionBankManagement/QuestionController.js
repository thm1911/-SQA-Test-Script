// PM_QBM_001
// Test lấy danh sách câu hỏi thành công
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

//Kiểm tra cấu trúc
pm.test("Response has correct structure", function () {
    pm.expect(body).to.have.property("statusCode");
    pm.expect(body).to.have.property("message");
    pm.expect(body).to.have.property("data");
});

// Kiểm tra code của body
pm.test("Business statusCode is 200", function () {
    pm.expect(body.statusCode).to.eql(200);
});

// Kiểm tra message
pm.test("Message is correct", function () {
    pm.expect(body.message).to.eql("Get question bank successfully!");
});

// Kiểm tra data là 1 danh sách
pm.test("Data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Kiểm tra data chứa các trường quan trọng id, questionText
pm.test("Each question has basic fields", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});


//---------------------------------------------------------------------------
// PM_QBM_002
// Test lấy danh sách câu hỏi thành công với danh sách trống
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

//Kiểm tra cấu trúc
pm.test("Response has correct structure", function () {
    pm.expect(body).to.have.property("statusCode");
    pm.expect(body).to.have.property("message");
    pm.expect(body).to.have.property("data");
});

// Kiểm tra code của body
pm.test("Business statusCode is 200", function () {
    pm.expect(body.statusCode).to.eql(200);
});

// Kiểm tra message
pm.test("Message is correct", function () {
    pm.expect(body.message).to.eql("Get question bank successfully!");
});

// Kiểm tra data là 1 danh sách
pm.test("Data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Kiểm tra data chứa các trường quan trọng id, questionText
pm.test("Each question has basic fields", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});

// Mock data là danh sách trống
const data = [];

pm.test("Data is an array", function () {
    pm.expect(Array.isArray(data)).to.eql(true);
});

pm.test("Array is empty", function () {
    pm.expect(data.length).to.eql(0);
});


//---------------------------------------------------------------------------
// PM_QBM_003
// Test lấy 1 câu hỏi theo id thành công
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const path = pm.request.url.getPath().split("/"); // có thể là string hoặc mảng
const pathId = path[path.length - 1]; // id ở segment cuối
const body = pm.response.json();
// Kiểm tra id tồn tại trong response
pm.test("Id exists", function () {
    pm.expect(body).to.have.property("id");
});

// Kiểm tra id giống với path id
pm.test("Id matches pathId", function () {
    pm.expect(body.id).to.eql(Number(pathId));
});

// Kiểm tra trả về trường question text
pm.test("QuestionText exists", function () {
    pm.expect(body).to.have.property("questionText");
});



//--------------------------------------------------------------------------- 
// PM_QBM_004
// Không tìm thấy câu hỏi theo id
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();
// Lấy đúng id ở cuối URL
const path = pm.request.url.getPath().split("/")
const pathId = path[path.length - 1];

// Kiểm tra code trong response
pm.test("StatusCode of response = 404", function () {
    pm.expect(body.statusCode).to.eql(404);
});

pm.test("Message format", function () {
    pm.expect(body.message).to.eql("Not found with id: " + pathId);
});

pm.test("Data = null", function () {
    pm.expect(body.data).to.eql(null);
});


//--------------------------------------------------------------------------- 
// PM_QBM_PART_005
// Kiểm tra lấy câu hỏi theo partId = 0 và role = ADMIN
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Response phải có data + paginationDetails
pm.test("Response has data & paginationDetails", function () {
    pm.expect(body).to.have.property("data");
    pm.expect(body).to.have.property("paginationDetails");
});

// Kiểm tra data là 1 danh sách
pm.test("data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Lấy partId từ URL
const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 2]); // .../parts/{partId}/questions
pm.test("partId in URL is 0", function () {
    pm.expect(partId).to.eql(0);
});

// Kiểm tra mỗi question tối thiểu có id & questionText
pm.test("Each question has id & questionText", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});

// Kiểm tra paginationDetails với page = 0 và số question/page = 10
const page = Number(pm.request.url.query.get("page"));
const size = Number(pm.request.url.query.get("size"));

pm.test("PaginationDetails has pageNumber = 0 & pageCount = 10", function () {
    pm.expect(body.paginationDetails.pageNumber).to.eql(page);
    pm.expect(body.paginationDetails.pageCount).to.eql(size);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_006
// Kiểm tra lấy câu hỏi theo partId = 0 và role = LECTURER
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Response phải có data + paginationDetails
pm.test("Response has data & paginationDetails", function () {
    pm.expect(body).to.have.property("data");
    pm.expect(body).to.have.property("paginationDetails");
});

// Kiểm tra data là 1 danh sách
pm.test("data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Lấy partId từ URL
const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 2]); // .../parts/{partId}/questions
pm.test("partId in URL is 0", function () {
    pm.expect(partId).to.eql(0);
});

// Kiểm tra mỗi question tối thiểu có id & questionText
pm.test("Each question has id & questionText", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});

// Kiểm tra paginationDetails với page = 0 và số question/page = 1
const page = Number(pm.request.url.query.get("page"));
const size = Number(pm.request.url.query.get("size"));

pm.test("PaginationDetails has pageNumber = 0 & pageCount = 1", function () {
    pm.expect(body.paginationDetails.pageNumber).to.eql(page);
    pm.expect(body.paginationDetails.pageCount).to.eql(1);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_007
// Kiểm tra lấy câu hỏi theo partId = 8 và role = ADMIN
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Response phải có data + paginationDetails
pm.test("Response has data & paginationDetails", function () {
    pm.expect(body).to.have.property("data");
    pm.expect(body).to.have.property("paginationDetails");
});

// Kiểm tra data là 1 danh sách
pm.test("data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Lấy partId từ URL
const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 2]); // .../parts/{partId}/questions
pm.test("partId in URL is 8", function () {
    pm.expect(partId).to.eql(8);
});

// Kiểm tra mỗi question tối thiểu có id & questionText
pm.test("Each question has id & questionText", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});

// Kiểm tra paginationDetails với page = 0 và số question/page = 2
const page = Number(pm.request.url.query.get("page"));
const size = Number(pm.request.url.query.get("size"));

pm.test("PaginationDetails has pageNumber = 0 & pageCount = 2", function () {
    pm.expect(body.paginationDetails.pageNumber).to.eql(page);
    pm.expect(body.paginationDetails.pageCount).to.eql(2);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_008
// Kiểm tra lấy câu hỏi theo partId = 8 và role = LECTURER
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Response phải có data + paginationDetails
pm.test("Response has data & paginationDetails", function () {
    pm.expect(body).to.have.property("data");
    pm.expect(body).to.have.property("paginationDetails");
});

// Kiểm tra data là 1 danh sách
pm.test("data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Lấy partId từ URL
const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 2]); // .../parts/{partId}/questions
pm.test("partId in URL is 8", function () {
    pm.expect(partId).to.eql(8);
});

// Kiểm tra mỗi question tối thiểu có id & questionText
pm.test("Each question has id & questionText", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});

// Kiểm tra paginationDetails với page = 0 và số question/page = 0
const page = Number(pm.request.url.query.get("page"));
const size = Number(pm.request.url.query.get("size"));

pm.test("PaginationDetails has pageNumber = 0 & pageCount = 0", function () {
    pm.expect(body.paginationDetails.pageNumber).to.eql(page);
    pm.expect(body.paginationDetails.pageCount).to.eql(0);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_009
// Kiểm tra lấy câu hỏi theo partId = 1000 => Not found
// Kiểm tra status code
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

const body = pm.response.json();

// Kiểm tra message
pm.test("Message is Part not found", function () {
    pm.expect(body.message).to.eql("Part not found");
});

// Kiểm tra data = null
pm.test("Data is null", function () {
    pm.expect(body.data).to.eql(null);
});



//---------------------------------------------------------------------------
// PM_QBM_PART_010
// Kiểm tra lấy câu hỏi với deleted = false theo partId = 8 và role = ADMIN
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Response phải có data + paginationDetails
pm.test("Response has data & paginationDetails", function () {
    pm.expect(body).to.have.property("data");
    pm.expect(body).to.have.property("paginationDetails");
});

// Kiểm tra data là 1 danh sách
pm.test("data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Lấy partId từ URL
const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 4]);
pm.test("partId in URL is 8", function () {
    pm.expect(partId).to.eql(8);
});

// Kiểm tra mỗi question tối thiểu có id & questionText
pm.test("Each question has id & questionText", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});

// Kiểm tra paginationDetails với page = 0 và số question/page = 2
const page = Number(pm.request.url.query.get("page"));
const size = Number(pm.request.url.query.get("size"));

pm.test("PaginationDetails has pageNumber = 0 & pageCount = 2", function () {
    pm.expect(body.paginationDetails.pageNumber).to.eql(page);
    pm.expect(body.paginationDetails.pageCount).to.eql(2);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_011
// Kiểm tra lấy câu hỏi với deleted = false theo partId = 8 và role != ADMIN
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Response phải có data + paginationDetails
pm.test("Response has data & paginationDetails", function () {
    pm.expect(body).to.have.property("data");
    pm.expect(body).to.have.property("paginationDetails");
});

// Kiểm tra data là 1 danh sách
pm.test("data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Lấy partId từ URL
const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 4]);
pm.test("partId in URL is 8", function () {
    pm.expect(partId).to.eql(8);
});

// Kiểm tra mỗi question tối thiểu có id & questionText
pm.test("Each question has id & questionText", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});

// Kiểm tra paginationDetails với page = 0 và số question/page = 0
const page = Number(pm.request.url.query.get("page"));
const size = Number(pm.request.url.query.get("size"));

pm.test("PaginationDetails has pageNumber = 0 & pageCount = 0", function () {
    pm.expect(body.paginationDetails.pageNumber).to.eql(page);
    pm.expect(body.paginationDetails.pageCount).to.eql(0);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_012
// Kiểm tra lấy câu hỏi với deleted = false theo partId = 1000 => Not found
// Kiểm tra status code
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

const body = pm.response.json();

// Kiểm tra message
pm.test("Message is Part not found", function () {
    pm.expect(body.message).to.eql("Part not found");
});

// Kiểm tra data = null
pm.test("Data is null", function () {
    pm.expect(body.data).to.eql(null);
});



//---------------------------------------------------------------------------
// PM_QBM_PART_013
// Kiểm tra lấy câu hỏi thành công
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

//Kiểm tra cấu trúc
pm.test("Response has correct structure", function () {
    pm.expect(body).to.have.property("statusCode");
    pm.expect(body).to.have.property("message");
    pm.expect(body).to.have.property("data");
});

// Kiểm tra code của body
pm.test("Business statusCode is 200", function () {
    pm.expect(body.statusCode).to.eql(200);
});

const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 2]);
// Kiểm tra message
pm.test("Message is correct", function () {
    pm.expect(body.message).to.eql("Get question list with question type id: " + partId);
});

// Kiểm tra data là 1 danh sách
pm.test("Data is an array", function () {
    pm.expect(Array.isArray(body.data)).to.eql(true);
});

// Kiểm tra data chứa các trường quan trọng id, questionText
pm.test("Each question has basic fields", function () {
    body.data.forEach((q, idx) => {
        pm.expect(q, `question[${idx}]`).to.have.property("id");
        pm.expect(q, `question[${idx}]`).to.have.property("questionText");
    });
});


//---------------------------------------------------------------------------
// PM_QBM_PART_014
// Kiểm tra lấy câu hỏi thành công với type không tồn tại
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Kiểm tra message
pm.test("Message is Not found question type with id: 1000", function () {
    pm.expect(body.message).to.eql("Not found question type with id: 1000");
});

// Kiểm tra data = null
pm.test("Data is null", function () {
    pm.expect(body.data).to.eql(null);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_015
// Kiểm tra tạo câu hỏi thành công
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

pm.test("Response has created question fields", function () {
    pm.expect(body).to.have.property("id");
    pm.expect(body).to.have.property("questionText");
    pm.expect(body).to.have.property("part");
    pm.expect(body).to.have.property("questionType");
    pm.expect(body).to.have.property("deleted");
});

pm.test("deleted default is false", function () {
    pm.expect(body.deleted).to.eql(false);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_016
// Kiểm tra tạo câu hỏi với questionText null/empty/blank
// Kiểm tra status code
pm.test("Status code is 400", function () {
    pm.response.to.have.status(400);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Question text is not empty", function () {
    pm.expect(body.message).to.eql("Question text is not empty");
});


//---------------------------------------------------------------------------
// PM_QBM_PART_017
// Kiểm tra tạo câu hỏi với nội dung đáp án trống
// Kiểm tra status code
pm.test("Status code is 400", function () {
    pm.response.to.have.status(400);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Choice text is not empty", function () {
    pm.expect(body.message).to.eql("Choice text is not empty");
});


//---------------------------------------------------------------------------
// PM_QBM_PART_018
// Kiểm tra tạo câu hỏi với danh sách đáp án trống
// Kiểm tra status code
pm.test("Status code is 400", function () {
    pm.response.to.have.status(400);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Choice is not empty", function () {
    pm.expect(body.message).to.eql("Choice is not empty");
});

//---------------------------------------------------------------------------
// PM_QBM_PART_019
// Kiểm tra tạo câu hỏi với question type không tồn tại
// Kiểm tra status code
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Question type is not found", function () {
    pm.expect(body.message).to.eql("Question type is not found");
});


//---------------------------------------------------------------------------
// PM_QBM_PART_020
// Kiểm tra tạo câu hỏi với part id không tồn tại
// Kiểm tra status code
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Part is not found", function () {
    pm.expect(body.message).to.eql("Part is not found");
});


//---------------------------------------------------------------------------
// PM_QBM_PART_021
// Kiểm tra cập nhật câu hỏi thành công
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();
const id = Number(pm.request.url.getPath().split("/").filter(Boolean).slice(-1)[0]);

pm.test("Business statusCode is 200", function () {
    pm.expect(body.statusCode).to.eql(200);
});

// Kiểm tra message
pm.test("Message is correct", function () {
    pm.expect(body.message).to.eql("Get question with id: " + id);
});

// Kiểm tra cấu trúc response với data có id match
pm.test("Data exists and id matched", function () {
    pm.expect(body).to.have.property("data");
    pm.expect(body.data).to.have.property("id");
    pm.expect(body.data.id).to.eql(id);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_022
// Kiểm tra cập nhật câu hỏi với question text trống
// Kiểm tra status code
pm.test("Status code is 400", function () {
    pm.response.to.have.status(400);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Question text is not empty", function () {
    pm.expect(body.message).to.eql("Question text is not empty");
});


//---------------------------------------------------------------------------
// PM_QBM_PART_023
// Kiểm tra cập nhật câu hỏi với choice text trống
// Kiểm tra status code
pm.test("Status code is 400", function () {
    pm.response.to.have.status(400);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Choice text is not empty", function () {
    pm.expect(body.message).to.eql("Choice text is not empty");
});


//---------------------------------------------------------------------------
// PM_QBM_PART_024
// Kiểm tra cập nhật câu hỏi với Choice là danh sách trống
// Kiểm tra status code
pm.test("Status code is 400", function () {
    pm.response.to.have.status(400);
});

const body = pm.message;
// Kiểm tra message
pm.test("Message is Choice is not empty", function () {
    pm.expect(body.message).to.eql("Choice is not empty");
});


//---------------------------------------------------------------------------
// PM_QBM_PART_025
// Kiểm tra cập nhật câu hỏi với Part không tồn tại
const body = pm.response.json();
const path = pm.request.url.getPath().split("/").filter(Boolean);
const partId = Number(path[path.length - 1]);

// Kiểm tra status code
pm.test("Business statusCode is 404", function () {
    pm.expect(body.statusCode).to.eql(404);
});

// Kiểm tra message
pm.test("Message is Not found with id: 1000", function () {
    pm.expect(body.message).to.eql("Not found with id: 1000");
});


// Kiểm tra data
pm.test("Data is null", function () {
    pm.expect(body.data).to.eql(null);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_026
// Kiểm tra xóa tạm câu hỏi với deleted = true
// Kiểm tra status code
pm.test("Status code is 204", function () {
    pm.response.to.have.status(204);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_027
// Kiểm tra xóa tạm câu hỏi với deleted = false
// Kiểm tra status code
pm.test("Status code is 204", function () {
    pm.response.to.have.status(204);
});


//---------------------------------------------------------------------------
// PM_QBM_PART_028
// Kiểm tra xóa tạm câu hỏi với id không tồn tại
// Kiểm tra status code
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

const body = pm.response.json();
// Kiểm tra message
pm.test("Message is Question not found", function () {
    pm.expect(body.message).to.eql("Question not found");
});
