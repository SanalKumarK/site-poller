const serviceTableContainer = document.querySelector('#service-table');
const deleteButton = document.querySelector('#delete-service');
const form = document.querySelector('#add-service-form');
const msg = document.querySelector('#msg');

let servicesRequest = new Request('/service');
let services;

reloadTable();
setInterval(function () {
    reloadTable();
}, 60000);

function reloadTable() {
    fetch(servicesRequest)
        .then(function (response) {
            return response.json();
        })
        .then(function (serviceList) {
            services = serviceList;
            services.forEach(service => {
                const rowId = service.name.replace(/\s/g, "_");
                var row = serviceTableContainer.querySelector('#' + rowId);
                if (row == null) {
                    row = document.createElement("tr");
                    row.id = rowId;
                    // creating checkbox element
                    var checkbox = document.createElement('input');
                    checkbox.type = "checkbox";
                    checkbox.value = service.url;
                    checkbox.id = "selected"
                    checkbox.onclick = evt => {
                        service.selected = evt.target.checked;
                    }
                    let name = document.createElement("td");
                    name.id = "name";
                    name.textContent = service.name;
                    let url = document.createElement("td");
                    url.id = "url";
                    url.textContent = service.url;
                    let status = document.createElement("td");
                    status.id = "status";
                    status.textContent = service.status;
                    let date = document.createElement("td");
                    date.id = "date";
                    date.textContent = service.date;

                    row.appendChild(document.createElement("td")).appendChild(checkbox);
                    row.appendChild(name);
                    row.appendChild(url);
                    row.appendChild(status);
                    row.appendChild(date);
                    serviceTableContainer.appendChild(row);
                } else {
                    service.selected = row.querySelector("#selected").checked;
                    row.querySelector("#name").textContent = service.name;
                    row.querySelector("#url").textContent = service.url;
                    row.querySelector("#status").textContent = service.status;
                    row.querySelector("#date").textContent = service.date;
                }
                row.querySelector("#selected").onclick = evt => {
                    service.selected = evt.target.checked;
                }
            });
        });
}

form.addEventListener("submit", function (event) {
    if (!form.checkValidity()) {
        return;
    }
    let name = document.querySelector('#name').value;
    let url = document.querySelector('#url').value;
    if (services.filter(service => service.name.toUpperCase() == name.toUpperCase()).length > 0) {
        setMessage("A service with given name already exists. Please modify the name!","error");
        event.preventDefault();
        return;
    }
    if (services.filter(service => service.url.toUpperCase() == url.toUpperCase()).length > 0) {
        setMessage("A service with given Url already exists. Please modify the url!", "error");
        event.preventDefault();
        return;
    }
    fetch('/service', {
        method: 'post',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({name: name.trim(), url: url.trim()})
    }).then(res => {
        res.text().then(value => {
            setMessage(value,"info");
            reloadTable();
        });
    });
    event.preventDefault();
});

deleteButton.onclick = evt => {
    var selectedUrls = services.filter(function (service) {
        return service.selected;
    }).map(value => value.url);
    if (selectedUrls.length == 0) {
        setMessage("Please select at least one service to delete.","info");
        return;
    }
    if (!confirm("Are you sure you want to delete the selected services?")) {
        return;
    }
    fetch('/service', {
        method: 'delete',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(selectedUrls)
    }).then(res => {
        res.text().then(value => {
            setMessage(value,"info");
            var selectedServices = services.filter(function (service) {
                return service.selected;
            }).map(value => value.name);
            selectedServices.forEach(service => {
                const rowId = service.replace(/\s/g, "_");
                const row = serviceTableContainer.querySelector('#' + rowId);
                row.remove();
            });
        });
        reloadTable();
    });
}

function setMessage(message, severity) {
    msg.textContent = message;
    if(severity == "error") {
        msg.setAttribute("style","color:red");
    }
    if(severity == "info") {
        msg.setAttribute("style","color:green");
    }
    setTimeout(function () {
        msg.textContent = "";
    }, 15000);
}