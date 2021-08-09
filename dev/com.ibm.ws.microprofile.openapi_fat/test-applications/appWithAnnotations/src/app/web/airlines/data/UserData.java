/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package app.web.airlines.data;

import java.util.ArrayList;
import java.util.List;

import app.web.airlines.model.User;

public class UserData {
    static List<User> users = new ArrayList<User>();

    static {
        users.add(createUser(1, "Bob1", "pswd", "Bob", "Smith", "male", 12,
                             "bobsm@test.com", "123-456-7890", 1));
        users.add(createUser(2, "Martha2", "pswd", "Martha", "Jones", "female", 30,
                             "marthaj@test.com", "123-456-7890", 2));
        users.add(createUser(3, "Jess3", "pswd", "Jessica", "Greene", "female", 18,
                             "jessgr@test.com", "123-456-7890", 3));
        users.add(createUser(4, "Tom4", "pswd", "Tom", "Brown", "male", 45,
                             "tomb@test.com", "123-456-7890", 1));
        users.add(createUser(5, "Jack4", "pswd", "Jack", "Thomson", "male", 34,
                             "jackth@test.com", "123-456-7890", 2));
        users.add(createUser(6, "Helga5", "pswd", "Helga", "Miller", "female", 76,
                             "helgam@test.com", "123-456-7890", 3));
        users.add(createUser(7, "Demi6", "pswd", "Demi", "Moore", "female", 121,
                             "demim@test.com", "123-456-7890", 1));
        users.add(createUser(8, "Emma7", "pswd", "Emma", "Watson", "female", 23,
                             "emmaw@test.com", "123-456-7890", 2));
        users.add(createUser(9, "Sherlock8", "pswd", "Sherlock", "Holmes", "male", 51,
                             "sherlockh@test.com", "123-456-7890", 3));
        users.add(createUser(10, "Doctor9", "pswd", "Doctor", "Frankenstein", "male", 81,
                             "franky@test.com", "123-456-7890", 1));
        users.add(createUser(11, "Marry10", "pswd", "Marry", "Shelly", "female", 47,
                             "marrysh@test.com", "123-456-7890", 1));

    }

    public User findUserByName(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public User findUserById(int id) {
        for (User user : users) {
            if (user.getId() == id) {
                return user;
            }
        }
        return null;
    }

    public void addUser(User user) {
        if (user.getUsername() == null) {
            return;
        }
        if (users.size() > 0) {
            for (int i = users.size() - 1; i >= 0; i--) {
                if (users.get(i).getUsername().equals(user.getUsername())) {
                    users.remove(i);
                }
            }
        }
        users.add(user);
    }

    public boolean removeUser(String username) {
        if (users.size() > 0) {
            for (int i = users.size() - 1; i >= 0; i--) {
                if (users.get(i).getUsername().equals(username)) {
                    users.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    private static User createUser(int id, String userName, String password, String firstName,
                                   String lastName, String sex, int age, String email, String phone, int status) {

        User user = new User(id, userName, password, firstName, lastName, sex, age, email, phone, status);

        return user;
    }
}
