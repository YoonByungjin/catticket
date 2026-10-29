Vagrant.configure("2") do |config|

  config.vm.define "gpt-server" do |cfg|
    cfg.vm.box = "bento/ubuntu-22.04"

    # 새 디스크 추가 (200GB)
    cfg.vm.disk :disk, size: "200GB" , name: "disk1"

    cfg.vm.provider "virtualbox" do |vb|
      vb.name = "gpt-server"
      vb.gui = true
      vb.cpus = 6
      vb.memory = 8192
    end

    cfg.vm.host_name = "gpt-server"
    cfg.vm.network "private_network", ip: "192.168.1.20"
    cfg.vm.network "forwarded_port", guest: 22, host: 60020, auto_correct: true, id: "ssh"
    cfg.vm.synced_folder "./", "/vagrant"

    cfg.vm.provision "shell", inline: <<-SHELL
      echo "[1/6] apt update"
      apt-get update -y

      echo "[2/6] Create PV on /dev/sdb"
      pvcreate /dev/sdb

      echo "[3/6] Extend VG (ubuntu-vg)"
      vgextend ubuntu-vg /dev/sdb

      echo "[4/6] Extend LV to 150G"
      lvextend -L 150G /dev/ubuntu-vg/ubuntu-lv

      echo "[5/6] Resize filesystem"
      resize2fs /dev/ubuntu-vg/ubuntu-lv

      echo "[6/6] Done. Disk usage:"
      df -h /
    SHELL
  end
end

