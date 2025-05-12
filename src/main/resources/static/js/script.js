$(document).ready(function () {

    console.log(typeof jQuery); // Should return "function"
    console.log(typeof jQuery.validator); // Should return "object"


    // Custom method to allow only letters
    $.validator.addMethod("onlyLetters", function (value, element) {
        return this.optional(element) || /^[a-zA-Z\s]+$/.test(value);
    }, "Only letters are allowed");

    // Custom method to allow only one space between words
    $.validator.addMethod("singleSpace", function (value, element) {
        return this.optional(element) || !/\s{2,}/.test(value);
    }, "Only a single space is allowed between words");

    // Validate mobile number (starts with 6-9, exactly 10 digits)
    $.validator.addMethod("mobileNumber", function (value, element) {
        return this.optional(element) || /^[6-9]\d{9}$/.test(value);
    }, "Enter a valid 10-digit mobile number");

    // Validate pincode (exactly 6 digits)
    $.validator.addMethod("pincodeValidation", function (value, element) {
        return this.optional(element) || /^\d{6}$/.test(value);
    }, "Enter a valid 6-digit Pincode");

    // Password strength (at least 6 characters, 1 number, 1 special character)
    $.validator.addMethod("passwordCheck", function (value, element) {
        return this.optional(element) || /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,}$/.test(value);
    }, "Password must be at least 6 characters, contain a number and a special character");

    // Confirm password check
    $.validator.addMethod("confirmPasswordCheck", function (value, element) {
        return value === $("#userRegister input[name='password']").val();
    }, "Passwords do not match");

    // Image validation (only JPG, JPEG, PNG)
    $.validator.addMethod("imageValidation", function (value, element) {
        if (value === "") return true; // Allow empty value
        return /\.(jpg|jpeg|png)$/i.test(value);
    }, "Only JPG, JPEG, or PNG images are allowed");

    $.validator.addMethod("validPayment", function(value, element) {
        return value !== "--select--";
    }, "Please select a valid payment method");
    
    // User Registration Validation
    $("#userRegister").validate({
        rules: {
            name: {
                required: true,
                onlyLetters: true,
                singleSpace: true
            },
            mobileNo: {
                required: true,
                mobileNumber: true
            },
            email: {
                required: true,
                email: true
            },
            address: {
                required: true,
                singleSpace: true
            },
            city: {
                required: true,
                onlyLetters: true,
                singleSpace: true
            },
            state: {
                required: true,
                onlyLetters: true,
                singleSpace: true
            },
            pincode: {
                required: true,
                pincodeValidation: true
            },
            password: {
                required: true,
                passwordCheck: true
            },
            cpassword: {
                required: true,
                confirmPasswordCheck: true
            },
            img: {
                required: false,
                imageValidation: true
            }
        },
        messages: {
            
            name: {
                required: "Please enter your name"
            },
            mobileNo: {
                required: "Please enter your mobile number"
            },
            email: {
                required: "Please enter your email",
                email: "Enter a valid email address"
            },
            address: {
                required: "Please enter your address"
            },
            city: {
                required: "Please enter your city"
            },
            state: {
                required: "Please enter your state"
            },
            pincode: {
                required: "Please enter your Pincode"
            },
            password: {
                required: "Please enter your password"
            },
            cpassword: {
                required: "Please confirm your password"
            },
            img: {
                required: "Please upload a valid image"
            }
        }
    });

    // Reset Password Validation
    $("#resetPassword").validate({
        rules: {
            password: {
                required: true,
                passwordCheck: true
            },
            confirm_password: {
                required: true,
                equalTo: "#resetPassword input[name='password']"
            }
        },
        messages: {
            password: "Please enter your new password",
            confirm_password: "Passwords do not match"
        }
    });

    // Order Form Validation
    $("#orderForm").validate({
        rules: {
            firstName: {
                required: true,
                onlyLetters: true,
                singleSpace: true
            },
            lastName: {
                required: true,
                onlyLetters: true
            },
            email: {
                required: true,
                email: true
            },
            mobileNo: {
                required: true,
                mobileNumber: true
            },
            address: {
                required: true,
                singleSpace: true
            },
            city: {
                required: true,
                onlyLetters: true,
                singleSpace: true
            },
            state: {
                required: true,
                onlyLetters: true,
                singleSpace: true
            },
            pincode: {
                required: true,
                pincodeValidation: true
            },
            paymentType: {
                required: true,
                validPayment: true
            }

        },
        messages: {
            firstName: {
                required: "Please enter your first name"
            },
            lastName: {
                required: "Please enter your last name"
            },
            email: {
                required: "Please enter your email",
                email: "Enter a valid email address"
            },
            mobileNo: {
                required: "Please enter your mobile number"
            },
            address: {
                required: "Please enter your address"
            },
            city: {
                required: "Please enter your city"
            },
            state: {
                required: "Please enter your state"
            },
            pincode: {
                required: "Please enter your Pincode"
            },
            paymentType: {
                required: "Please select a payment method"
            }
        }
    });

    $("#addProductForm").validate({
        rules: {
            title: {
                required: true,
                minlength: 3
            },
            description: {
                required: true
            },
            category: {
                required: true
            },
            price: {
                required: true,
                number: true,
                min: 1
            },
            stock: {
                required: true,
                number: true,
                min: 0
            },
            file: {
                required: false,
                imageValidation: true
            }
        },
        messages: {
            title: {
                required: "Please enter a product title",
                minlength: "Title must be at least 3 characters"
            },
            description: {
                required: "Please enter a description"
            },
            category: {
                required: "Please select a category"
            },
            price: {
                required: "Please enter a price",
                number: "Price must be a valid number",
                min: "Price must be greater than 0"
            },
            stock: {
                required: "Please enter stock quantity",
                number: "Stock must be a valid number",
                min: "Stock cannot be negative"
            },
            file: {
                required: "Please upload a valid image"
            }
        }
    });
});
