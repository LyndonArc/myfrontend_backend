<template>
  <div>
    <h1>Role Management</h1>
    <div>
      <input v-model="searchRoleName" placeholder="Search role name">
      <button @click="searchRole">Search</button>
    </div>
    <div v-if="searchedRole">
      <h3>Search Result:</h3>
      <p>{{ searchedRole.roleName }} - {{ searchedRole.description }}</p>
    </div>
    <div>
      <input v-model="newRole.roleName" placeholder="Enter role name">
      <input v-model="newRole.description" placeholder="Enter description">
      <input type="file" @change="handleFileUpload"> <!-- 新增文件输入框 -->
      <button @click="createRole">Add Role</button>
    </div>

    <!-- 表格显示 -->
    <table>
      <thead>
        <tr>
          <th><input type="checkbox" @click="toggleAllSelections" :checked="isAllSelected"></th>
          <th>ID</th>
          <th>Role Name</th>
          <th>Description</th>
          <th>Avatar</th> <!-- 新增头像列 -->
          <th>Created At</th>
          <th>Updated At</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="role in roles.content" :key="role.id">
          <td><input type="checkbox" :value="role.id" v-model="selectedRoles"></td>
          <!-- 添加显示ID的列 -->
          <td>{{ role.id }}</td>
          <td v-if="!editMode || editRoleData.id !== role.id">{{ role.roleName }}</td>
          <td v-else>
            <input v-model="editRoleData.roleName" placeholder="Enter new role name">
          </td>
          <td>{{ role.description }}</td>
          <td><img v-if="role.avatarPath" :src="role.avatarPath" alt="Avatar" width="50"></td>
          <td>{{ formatDate(role.createdAt) }}</td>
          <td>{{ formatDate(role.updatedAt) }}</td>
          <td>
            <button v-if="editMode && editRoleData.id === role.id" @click="updateRole">Save</button>
            <button v-else @click="editRole(role)">Edit</button>
            <button @click="deleteRole(role.id)">Delete</button>
          </td>
        </tr>
      </tbody>

    </table>

    <button @click="batchDeleteRoles">Delete Selected Roles</button>

    <!-- Pagination Controls -->
    <div class="pagination">
      <button @click="previousPage" :disabled="currentPage === 0">Previous</button>
      <span>Page {{ currentPage + 1 }} of {{ roles.totalPages }}</span>
      <button @click="nextPage" :disabled="currentPage >= roles.totalPages - 1">Next</button>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      roles: {}, // 用于存储分页数据
      selectedRoles: [],
      newRole: { roleName: '', description: '', avatarPath: '' },
      editMode: false,
      editRoleData: { id: null, roleName: '', description: '', avatarPath: '' },
      originalAvatarPath: '', // 新增字段，用于保存原始的 avatarPath
      searchRoleName: '',
      searchedRole: null,
      currentPage: 0, // 当前页码
      pageSize: 4, // 每页显示的条目数
      avatarFile: null // 存储选择的文件
    };
  },
  computed: {
    isAllSelected() {
      return this.roles.content && this.selectedRoles.length === this.roles.content.length;
    }
  },
  methods: {
    fetchRoles() {
      axios.get('http://localhost:8080/api/roles/page', {
        params: {
          page: this.currentPage,
          size: this.pageSize
        }
      })
      .then(response => {
        this.roles = response.data;
        console.log(this.roles.content);
        // 为每个角色设置完整的 Avatar URL
        this.roles.content.forEach(role => {
          if (role.avatarPath) {
            role.avatarPath = `http://localhost:8080${role.avatarPath}`;
          }
        });
      });
    },
    searchRole() {
      axios.get('http://localhost:8080/api/roles/search', {
        params: { roleName: this.searchRoleName }
      })
      .then(response => {
        this.searchedRole = response.data;
      });
    },
    handleFileUpload(event) {
      this.avatarFile = event.target.files[0]; // 保存选中的文件
    },
    createRole() {
      axios.post('http://localhost:8080/api/roles', this.newRole)
      .then(response => {
        const roleId = response.data.id; // 获取新创建的角色 ID
        this.newRole.roleName = '';
        this.newRole.description = '';
        this.newRole.avatarPath = '';

        if (this.avatarFile) {
          this.uploadAvatar(roleId); // 如果选择了头像，上传头像
        } else {
          this.fetchRoles(); // 如果没有选择头像，直接刷新角色列表
        }
      })
      .catch(error => {
        console.error('Error creating role:', error);
      });
    },
    uploadAvatar(roleId) {
      const formData = new FormData();
      formData.append('file', this.avatarFile);

      axios.post(`http://localhost:8080/api/roles/upload-avatar/${roleId}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      .then(response => {
        // 确保更新后的 avatarPath 被保存
        this.editRoleData.avatarPath = response.data; 
        this.avatarFile = null;
        this.editMode = false;
        this.fetchRoles(); // 上传头像成功后刷新角色列表
      })
      .catch(error => {
        console.error('Error uploading avatar:', error);
      });
    },

    editRole(role) {
      this.editMode = true;
      this.editRoleData = { ...role };
      this.originalAvatarPath = role.avatarPath; // 保存原始的 avatarPath
    },
    updateRole() {
    const updatedRole = {
      roleName: this.editRoleData.roleName,
      description: this.editRoleData.description,
    };

    // 如果用户选择了新的头像文件，则包含 avatarPath
    if (this.avatarFile) {
      updatedRole.avatarPath = this.editRoleData.avatarPath;
    }

    axios.put(`http://localhost:8080/api/roles/${this.editRoleData.id}`, updatedRole)
      .then(() => {
        if (this.avatarFile) {
          this.uploadAvatar(this.editRoleData.id); // 如果选择了头像，上传头像
        } else {
          this.editMode = false;
          this.editRoleData = { id: null, roleName: '', description: '', avatarPath: '' };
          this.avatarFile = null;
          this.fetchRoles(); // 直接刷新角色列表
        }
      })
      .catch(error => {
        console.error('Error updating role:', error);
      });
  },


    deleteRole(id) {
      axios.delete(`http://localhost:8080/api/roles/${id}`)
      .then(() => {
        this.fetchRoles();
      });
    },
    batchDeleteRoles() {
      axios.delete('http://localhost:8080/api/roles/batch-delete', {
        data: this.selectedRoles
      })
      .then(() => {
        this.selectedRoles = [];
        this.fetchRoles();
      });
    },
    toggleAllSelections() {
      if (this.isAllSelected) {
        this.selectedRoles = [];
      } else {
        this.selectedRoles = this.roles.content.map(role => role.id);
      }
    },
    // 分页控件的操作方法
    nextPage() {
      if (this.currentPage < this.roles.totalPages - 1) {
        this.currentPage++;
        this.fetchRoles();
      }
    },
    previousPage() {
      if (this.currentPage > 0) {
        this.currentPage--;
        this.fetchRoles();
      }
    },
    // 日期格式化方法
    formatDate(date) {
      if (!date) return '';
      const options = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' };
      return new Date(date).toLocaleDateString('zh-CN', options);
    }
  },
  created() {
    this.fetchRoles(); // 初始化时获取第一页数据
  }
};
</script>

<style scoped>
button {
  margin-left: 10px;
}
input {
  margin-bottom: 10px;
}
.pagination {
  margin-top: 20px;
}
.pagination button {
  margin: 0 5px;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

th, td {
  border: 1px solid #ddd;
  padding: 8px;
}

th {
  background-color: #f2f2f2;
  text-align: center;
}

td {
  text-align: center;
}
</style>
