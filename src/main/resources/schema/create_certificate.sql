CREATE TABLE CERTIFICATE (
    SEQ VARCHAR(10) PRIMARY KEY,
    NAME VARCHAR(255) NOT NULL,
    TYPE VARCHAR(255) NOT NULL,
    AGENCY VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;