-----DB 구조------
CREATE TABLE customer (
    phone_num CHAR(11) PRIMARY KEY,
    name VARCHAR(10) NOT NULL,
    id VARCHAR(15) NOT NULL,
    pw VARCHAR(15) NOT NULL,
    idtf_num CHAR(13) NOT NULL,
    time TIME,
    seat INT DEFAULT 0,  -- seat 디폴트 값 = 0으로 설정
    login_time DATETIME,
    logout_time DATETIME
);

CREATE TABLE food_order (
    order_id INT PRIMARY KEY,
    seat_num INT NOT NULL,
    food_name VARCHAR(50) NOT NULL,
    FOREIGN KEY (seat_num) REFERENCES customer(seat),
    FOREIGN KEY (food_name) REFERENCES food(name)
);

CREATE TABLE food (
    name VARCHAR(10) PRIMARY KEY,
    price INT NOT NULL,
    foodtype VARCHAR(10) NOT NULL
);

CREATE TABLE sales (
    day DATE PRIMARY KEY,
    sales INT NOT NULL
);
