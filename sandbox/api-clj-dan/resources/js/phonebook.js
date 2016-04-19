log = function(msg, e) {
    console.log(msg + ":= " + e)
    console.dir(e)
}

phonebook = {
    delete: function(url) {
        x = new XMLHttpRequest()
        x.onload = function(e) {
            if (e.target.status == 200) {
                // index is set by the server
                window.location.pathname = index
            }
        }
        x.onerror = function(e) { log("onerror is ", e)}
        x.open("DELETE", url)
        x.send("")
    },
    update: function(url) {
        x = new XMLHttpRequest()
        x.onload = function(e) {
            if (e.target.status == 204) {
                // entry is set by the server
                window.location.pathname = entry
            }
        }
        x.onerror = function(e) { log("onerror is ", e)}
        x.open("PUT", url)
        x.send(new FormData(document.getElementById("entry")))
    }
}
