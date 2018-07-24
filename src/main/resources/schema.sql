drop table if exists  reservations ;
create table reservations (
  id               bigint       not null  auto_increment primary key,
  reservation_name varchar(255) not null
);