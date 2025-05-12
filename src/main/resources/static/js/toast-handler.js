document.addEventListener("DOMContentLoaded", function() {
    let successToast = document.getElementById('successToast');
    let errorToast = document.getElementById('errorToast');

    if (successToast) {
        let toast = new bootstrap.Toast(successToast);
        toast.show();
    }

    if (errorToast) {
        let toast = new bootstrap.Toast(errorToast);
        toast.show();
    }
});
