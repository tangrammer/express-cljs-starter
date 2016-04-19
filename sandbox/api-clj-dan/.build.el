;; project settings
(setq ent-project-home (file-name-directory (if load-file-name load-file-name buffer-file-name)))
(setq ent-project-name "starbucks")
(setq ent-clean-regexp "~$\\|.*\\.lo[ft]$")
(setq ent-project-config-filename "Starbucks.org")

;; local functions
(defvar project-version)

(setq project-version (ent-get-version))


;; tasks

(load ent-init-file)

(task 'check '() "check the project" '(lambda (&optional x) "lein with-profile +check checkall"))

(task 'update () "update project libraries" '(lambda (&optional x) "lein ancient :no-colors"))

(task 'tree '() "tree dependencies" '(lambda (&optional x) "lein do clean, deps :tree"))

(task 'tests '() "run tests" '(lambda (&optional x) "lein test"))


;; Local Variables:
;; no-byte-compile: t
;; no-update-autoloads: t
;; End:
