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

package app.web.airlines.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(maxProperties = 1024, minProperties = 1, requiredProperties = {
    "id", "username", "password"
})
public class User {

    @Schema(example = "3456")
    private int id;

    @Schema(example = "user1")
    private String userName;

    @Schema(example = "bobSm37")
    private String password;

    @Schema(required = true, example = "Bob")
    private String firstName;

    @Schema(required = true, example = "Smith")
    private String lastName;

    @Schema(required = true, example = "M")
    private String sex;

    @Schema(required = true, example = "37")
    private int age;

    @Schema(required = true, example = "bob@test.ca")
    private String email;

    @Schema(required = true, example = "123-456-7890")
    private String phone;

    @Schema(required = true, example = "1")
    private int status;

    // Note, multipleof=0.1 caused parsing errors in earlier versions of jandex
    @Schema(required = false, example = "170.1", multipleOf = 0.1, description = "Height in cm")
    private double height;

    /**
     * Creates a User instance with the parameters specified.
     *
     * @param id        the unique id for this User instance
     * @param userName  the unique username for this User instance
     * @param password  the unique password for this User instance
     * @param firstName the first name for this User instance
     * @param lastName  the last name for this User instance
     * @param sex       the sex for this User instance
     * @param age       the age value for this User instance
     * @param email     the email associated with this User instance
     * @param phone     the phone number associated with this User instance
     * @param status    the status associated with this User instance
     */
    public User(int id, String userName, String password, String firstName,
        String lastName, String sex, int age, String email, String phone, int status) {
        super();
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = sex;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.status = status;
    }

    /**
     * Returns the id property of a User instance
     *
     * @return int id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the id property of a User instance to the parameter.
     *
     * @param id the unique id for this User instance
     */
    public void setId(
        int id) {
        this.id = id;
    }

    /**
     * Returns the userName property of a User instance.
     *
     * @return String userName
     */
    public String getUsername() {
        return userName;
    }

    /**
     * Sets the userName property of a User instance to the parameter.
     *
     * @param userName the user name for this User instance
     */
    public void setUsername(
        String userName) {
        this.userName = userName;
    }

    /**
     * Returns the password property of a User instance.
     *
     * @return String password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password property of a User instance to the parameter.
     *
     * @param password the password associated with this User instance
     */
    public void setPassword(
        String password) {
        this.password = password;
    }

    /**
     * Returns firstName property of a User instance.
     *
     * @return String firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the firstName property of a User instance to the parameter.
     *
     * @param firstName the first name for this User instance
     */
    public void setFirstName(
        String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the lastName property of a User instance.
     *
     * @return String lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the lastName property of a User instance to the parameter.
     *
     * @param lastName the last name for this User instance
     */
    public void setLastName(
        String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the sex property of a User instance.
     *
     * @return String sex
     */
    public String getSex() {
        return sex;
    }

    /**
     * Sets the sex property of a User instance to the parameter.
     *
     * @param sex the sex for this User instance
     */
    public void setSex(
        String sex) {
        this.sex = sex;
    }

    /**
     * Returns the age property of a User instance.
     *
     * @return int age
     */
    public int getAge() {
        return age;
    }

    /**
     * Sets the age property of a User instance to the parameter.
     *
     * @param age the age value for this User instance
     */
    public void setAge(
        int age) {
        this.age = age;
    }

    /**
     * Returns the email property of a User instance.
     *
     * @return String email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email property of a User instance to the parameter.
     *
     * @param email the email associated with this User instance
     */
    public void setEmail(
        String email) {
        this.email = email;
    }

    /**
     * Returns the phone property of a User instance.
     *
     * @return String phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the phone property of a User instance to the parameter.
     *
     * @param phone the phone number associated with this User instance
     */
    public void setPhone(
        String phone) {
        this.phone = phone;
    }

    @Schema(name = "status", title = "User Status")

    /**
     * Returns the status of this User instance.
     *
     * @return the integer associated with this User's status
     */
    public int getUserStatus() {
        return status;
    }

    /**
     * Sets the status of this User instance.
     *
     * @param status an integer representing the status of this User instance
     */
    public void setUserStatus(
        int status) {
        this.status = status;
    }

    /**
     * @return the height
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(
        double height) {
        this.height = height;
    }

}
