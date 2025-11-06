const API_BASE_URL = 'http://localhost:8080/api';

class API {
    // Products
    static async getAllProducts() {
        const response = await fetch(`${API_BASE_URL}/products`);
        return await response.json();
    }

    static async getProduct(id) {
        const response = await fetch(`${API_BASE_URL}/products/${id}`);
        return await response.json();
    }

    static async searchProducts(category) {
        const response = await fetch(`${API_BASE_URL}/products/search?category=${category}`);
        return await response.json();
    }

    // Cart
    static async addToCart(productId, quantity) {
        const response = await fetch(`${API_BASE_URL}/cart/add`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId, quantity }),
            credentials: 'include' // for session management
        });
        return await response.json();
    }

    static async getCart() {
        const response = await fetch(`${API_BASE_URL}/cart`, {
            credentials: 'include'
        });
        return await response.json();
    }

    static async removeFromCart(productId) {
        const response = await fetch(`${API_BASE_URL}/cart/remove/${productId}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        return await response.json();
    }

    // Checkout
    static async checkout(customerData, paymentMethod) {
        const response = await fetch(`${API_BASE_URL}/orders/checkout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ customerData, paymentMethod }),
            credentials: 'include'
        });
        return await response.json();
    }
}