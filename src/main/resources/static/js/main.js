// main.js

document.addEventListener("DOMContentLoaded", () => {

    const loginModalElement = document.getElementById('loginModal');
    const registerModalElement = document.getElementById('registerModal');

    if (loginModalElement && registerModalElement) {
        loginModalElement.addEventListener('hidden.bs.modal', function () {

        });

        registerModalElement.addEventListener('hidden.bs.modal', function () {

        });
    }

});
