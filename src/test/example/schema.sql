--
-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at http://mozilla.org/MPL/2.0/.

drop table if exists spacesuit.place;

drop schema if exists spacesuit;

create schema spacesuit;

use spacesuit;

create table place(
    id int not null auto_increment,
    latitude double not null,
    longitude double not null,
    description varchar(200) not null,
    z bigint not null,
    primary key(id)
);

create index idx_place_z on place(z);
